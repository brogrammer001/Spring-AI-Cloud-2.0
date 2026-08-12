package com.mall.aichat.service.impl;

import com.mall.aichat.domain.KbDocument;
import com.mall.aichat.domain.KbKnowledgeBase;
import com.mall.aichat.service.IKbDocumentService;
import com.mall.aichat.service.IKbKnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用RAG检索服务
 * <p>
 * 对标 Dify Knowledge API 的检索模式，统一管理向量检索与 Reranker 重排序逻辑。
 * 通过 kbType 参数区分不同业务场景：
 * - kbType=10: ChatAgent 通用知识检索
 * - kbType=20: NL2SQL 表结构专业知识检索
 * <p>
 * 采用「反向匹配 + 双重过滤」的两步检索策略：
 * 1. 反向匹配：查询所有文档的 tags 值 → 检查 question 是否包含某个 tag → 获取精确的 tags 和 knowledgeId
 * 2. 向量检索：用 tags IN (...) AND knowledgeId IN (...) 双重过滤向量库，提升精度
 * <p>
 * 反向匹配相比正向关键词提取的优势：
 * - 无需分词，准确率100%（直接字符串包含）
 * - 零延迟（纯内存计算）
 * - 零依赖（无需 Lucene/HanLP 等NLP库）
 */
@Service
public class RagRetrieveContextService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrieveContextService.class);

    @Autowired(required = false)
    @Qualifier("knowledgeVectorStore")
    private VectorStore knowledgeVectorStore;

    @Autowired
    private IKbKnowledgeBaseService kbKnowledgeBaseService;

    @Autowired
    private IKbDocumentService kbDocumentService;

    @Autowired
    private RerankerService rerankerService;

    /**
     * 根据问题和知识库类型从向量库检索相关文档片段
     * <p>
     * 两步检索策略（tags + knowledgeId 双重过滤）：
     * 1. 反向匹配：查询所有文档的 tags → 检查 question 是否包含 tag → 获取精确 tags 值和 knowledgeId
     * 2. 向量检索：用 tags IN (...) AND knowledgeId IN (...) 双重过滤向量库，提升精度
     *
     * @param question 用户问题
     * @param kbType   知识库类型（10-通用知识, 20-表结构专业知识, ...）
     * @return 检索到的文本片段，未检索到返回空字符串
     */
    public String retrieveContext(String question, String kbType) {
        if (knowledgeVectorStore == null) {
            log.warn("[RAG检索] 知识库向量库未配置，跳过检索");
            return "";
        }
        if (question == null || question.trim().isEmpty()) {
            return "";
        }

        FilterExpressionBuilder b = new FilterExpressionBuilder();

        // ========== 第一步：反向匹配 → 获取精确的 tags 和 knowledgeId ==========
        String[] knowledgeIds;
        String[] tagValues = null;

        // 查询 kbType 下所有有 tags 的文档
        List<KbDocument> allTaggedDocs = kbDocumentService.selectDocumentsByTags(null, kbType, "0");
        log.info("[RAG检索] kbType={} 查询到{}个带tags的文档", kbType, allTaggedDocs == null ? 0 : allTaggedDocs.size());

        // 反向匹配：检查 question 是否包含某个 tag
        List<KbDocument> matchedDocs = matchDocsByReverseTag(question, allTaggedDocs);

        if (!matchedDocs.isEmpty()) {
            // 标签匹配成功，提取精确的 tags 值（向量库 metadata 中的完整字符串）和 knowledgeId
            Set<String> tagSet = new LinkedHashSet<>();
            Set<String> kbIdSet = new LinkedHashSet<>();
            for (KbDocument doc : matchedDocs) {
                if (doc.getTags() != null && !doc.getTags().isEmpty()) {
                    tagSet.add(doc.getTags());
                }
                if (doc.getKnowledgeId() != null) {
                    kbIdSet.add(doc.getKnowledgeId());
                }
            }
            tagValues = tagSet.toArray(new String[0]);
            knowledgeIds = kbIdSet.toArray(new String[0]);
            log.info("[RAG检索] 反向匹配成功，命中{}个文档，{}个tags值，{}个知识库",
                matchedDocs.size(), tagValues.length, knowledgeIds.length);
        } else {
            log.info("[RAG检索] 反向匹配未命中，降级为全量检索（仅knowledgeId过滤）");
            knowledgeIds = getAllKnowledgeIdsByType(kbType);
        }

        if (knowledgeIds.length == 0) {
            log.info("[RAG检索] kbType={} 无启用的知识库", kbType);
            return "";
        }

        // ========== 第二步：向量检索（tags + knowledgeId 双重过滤）+ Reranker ==========
        Filter.Expression expression;
        if (tagValues != null && tagValues.length > 0) {
            // 双重过滤：tags IN (...) AND knowledgeId IN (...)
            expression = b.and(
                b.in("tags", tagValues),
                b.in("knowledgeId", knowledgeIds)
            ).build();
            log.info("[RAG检索] 启用双重过滤：tags IN {} AND knowledgeId IN {}", tagValues.length, knowledgeIds.length);
        } else {
            // 仅 knowledgeId 过滤（降级场景）
            expression = b.in("knowledgeId", knowledgeIds).build();
        }

        boolean rerankEnabled = rerankerService.isEnabled();
        int retrieveTopK = rerankEnabled ? 10 : 3;
        double similarityThreshold = rerankEnabled ? 0.5 : 0.7;
        SearchRequest request = SearchRequest.builder()
            .query(question)
            .topK(retrieveTopK)
            .similarityThreshold(similarityThreshold)
            .filterExpression(expression)
            .build();

        List<Document> results = knowledgeVectorStore.similaritySearch(request);
        if (results == null || results.isEmpty()) {
            log.info("[RAG检索] kbType={} 向量检索未命中任何片段", kbType);
            return "";
        }

        // Reranker 重排序
        List<Document> finalResults;
        if (rerankEnabled && results.size() > 1) {
            List<String> docTexts = results.stream().map(Document::getText).toList();
            List<Integer> rerankedIndices = rerankerService.rerank(question, docTexts);

            if (!rerankedIndices.isEmpty()) {
                finalResults = new ArrayList<>();
                for (int idx : rerankedIndices) {
                    if (idx < results.size()) {
                        finalResults.add(results.get(idx));
                    }
                }
                log.info("[RAG检索] kbType={} 向量检索{}条 → 重排序后取{}条", kbType, results.size(), finalResults.size());
            } else {
                finalResults = results.subList(0, Math.min(3, results.size()));
                log.warn("[RAG检索] kbType={} Reranker降级，使用向量检索前{}条", kbType, finalResults.size());
            }
        } else {
            finalResults = results.subList(0, Math.min(3, results.size()));
        }

        return finalResults.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 反向匹配：检查 question 是否包含某个 tag
     * <p>
     * 相比正向关键词提取（extractKeywords）的优势：
     * - 准确率100%：直接字符串包含检查，无分词误差
     * - 零延迟：纯内存计算，无 LLM/NLP 调用
     * - 零依赖：无需 Lucene/HanLP/jieba 等分词库
     *
     * @param question 用户问题
     * @param docs     所有带 tags 的文档列表
     * @return 匹配的文档列表（question 包含某个 tag）
     */
    private List<KbDocument> matchDocsByReverseTag(String question, List<KbDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        List<KbDocument> matched = new ArrayList<>();
        for (KbDocument doc : docs) {
            if (doc.getTags() == null || doc.getTags().trim().isEmpty()) {
                continue;
            }

            // 按逗号分割 tags（兼容中英文逗号）
            String[] tagArray = doc.getTags().split("[,，]");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                // tag 至少2个字符才匹配，避免单字误匹配
                if (trimmedTag.length() >= 2 && question.contains(trimmedTag)) {
                    matched.add(doc);
                    log.info("[RAG检索] 反向匹配命中: tag='{}' ∈ question='{}'", trimmedTag, question);
                    break; // 一个 tag 匹配即可，避免重复添加
                }
            }
        }
        return matched;
    }

    /**
     * 获取指定类型的所有启用知识库ID
     */
    private String[] getAllKnowledgeIdsByType(String kbType) {
        KbKnowledgeBase kbKnowledgeBase = new KbKnowledgeBase();
        kbKnowledgeBase.setStatus("0");
        kbKnowledgeBase.setKbType(kbType);
        List<KbKnowledgeBase> kbKnowledgeBases = kbKnowledgeBaseService.selectKbKnowledgeBaseList(kbKnowledgeBase);

        if (kbKnowledgeBases.isEmpty()) {
            return new String[0];
        }
        return kbKnowledgeBases.stream().map(KbKnowledgeBase::getId).toArray(String[]::new);
    }

    /**
     * 兼容旧调用方式：默认 kbType=10（通用知识）
     */
    public String retrieveContext(String question) {
        return retrieveContext(question, "10");
    }
}
