package com.mall.aichat.service.impl;

import com.mall.aichat.domain.KbDocument;
import com.mall.aichat.service.IKbDocumentService;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用RAG检索服务
 * <p>
 * 对标 Dify Knowledge API 的检索模式，统一管理向量检索与 Reranker 重排序逻辑。
 * 通过 kbType 参数区分不同业务场景：
 * - kbType=10: ChatAgent 通用知识检索
 * - kbType=20: NL2SQL 表结构专业知识检索
 * <p>
 * 执行流程（用户消息 → 知识库标签匹配 → 获取 knowledgeId → 向量检索 → Reranker 重排序）：
 * 1. 知识库前置检查：查询 kbType 下是否有启用的知识库，无则跳过向量检索
 * 2. 反向匹配：获取 kbType 下所有文档，在内存中按 knowledgeId 过滤 → 检查用户消息是否包含某个 tag → 找出所有相似的 KbDocument
 * 3. 向量检索：从匹配的 KbDocument 获取 getKnowledgeId，用 tags IN (...) AND knowledgeId IN (...) 双重过滤向量库
 * 4. 重排序：对向量检索结果用 Reranker 二次排序，返回最相关的片段
 * <p>
 * 反向匹配相比正向关键词提取的优势：
 * - 无需分词，准确率100%（直接字符串包含）
 * - 零延迟（纯内存计算）
 * - 零依赖（无需 Lucene/HanLP 等NLP库）
 */
@Service
public class RagRetrieveContextService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrieveContextService.class);

    /** Reranker 启用时最终返回的TopN */
    private static final int TOP_N = 3;

    @Autowired(required = false)
    @Qualifier("knowledgeVectorStore")
    private VectorStore knowledgeVectorStore;

    @Autowired
    private IKbDocumentService kbDocumentService;

    @Autowired
    private RerankerService rerankerService;

    /**
     * 根据问题和知识库类型从向量库检索相关文档片段
     * <p>
     * 三步检索策略（知识库前置检查 → 反向匹配 → tags + knowledgeId 双重过滤 → Reranker 重排序）：
     * 1. 知识库前置检查：查询 kbType 下是否有启用的知识库，无则跳过向量检索
     * 2. 反向匹配：获取 kbType 下所有文档，在内存中按 knowledgeId 过滤 → 匹配 question 包含的 tag → 找出所有相似的 KbDocument
     * 3. 向量检索：从匹配文档获取 getKnowledgeId，用 tags IN (...) AND knowledgeId IN (...) 双重过滤向量库
     * 4. 重排序：对向量检索结果用 Reranker 二次排序，返回最相关的片段
     *
     * @param question 用户问题
     * @param kbType   知识库类型（10-通用知识, 20-表结构专业知识, ...）
     * @return 检索到的文本片段，未检索到返回空字符串
     */
    public String retrieveContext(String question, String kbType) {
        // 前置校验
        if (!validateInput(question, kbType)) {
            return "";
        }

        // ========== 第一步：标签匹配 → 找出所有相似 KbDocument，获取精确 tags 和 knowledgeId ==========
        MatchResult matchResult = matchDocumentsByTags(question, kbType);
        if (!matchResult.hasKnowledgeIds()) {
            log.info("[RAG检索] kbType={} 无启用的知识库，跳过检索", kbType);
            return "";
        }

        // ========== 第二步：向量检索（tags + knowledgeId 双重过滤） ==========
        List<Document> results = vectorSearch(question, matchResult);
        if (results.isEmpty()) {
            log.info("[RAG检索] kbType={} 向量检索未命中任何片段", kbType);
            return "";
        }

        // ========== 第三步：Reranker 重排序 ==========
        List<Document> finalResults = rerankAndTrim(question, results);

        return finalResults.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 前置校验：向量库配置、问题、知识库类型
     */
    private boolean validateInput(String question, String kbType) {
        if (knowledgeVectorStore == null) {
            log.warn("[RAG检索] 知识库向量库未配置，跳过检索");
            return false;
        }
        if (question == null || question.trim().isEmpty()) {
            log.warn("[RAG检索] question为空，跳过检索");
            return false;
        }
        if (kbType == null || kbType.trim().isEmpty()) {
            log.warn("[RAG检索] kbType为空，跳过检索");
            return false;
        }
        return true;
    }

    /**
     * 第一步：标签匹配 → 找出所有相似的 KbDocument
     * <p>
     * 匹配策略（三级降级）：
     * 1. 知识库前置检查：先查 kbType 下是否有启用的知识库，无则直接返回空（跳过向量检索）
     * 2. 反向匹配：获取 kbType 下所有文档，在内存中按 knowledgeId 过滤 → 检查 question 是否包含某个 tag（准确率100%）
     * 3. 模糊匹配：若反向匹配未命中，将 question 拆分为关键词，在内存中检查 tags 是否包含任一关键词
     * 4. 全量降级：若标签匹配均未命中，退化为仅 knowledgeId 过滤的全量检索
     * <p>
     * 匹配成功后，提取精确的 tags 值（向量库 metadata 中的完整字符串）和 knowledgeId
     *
     * @return 匹配结果（包含 tags 过滤值和 knowledgeIds）
     */
    private MatchResult matchDocumentsByTags(String question, String kbType) {
        // ========== 第一步：关联查询 kbType 下所有启用知识库的文档（一次SQL） ==========
        List<KbDocument> filteredDocs = kbDocumentService.selectDocumentsByKbType(kbType, "0");
        if (filteredDocs == null || filteredDocs.isEmpty()) {
            log.info("[RAG检索] kbType={} 无启用的知识库，跳过检索", kbType);
            return new MatchResult(null, null);
        }

        // 提取知识库ID集合（用于全量降级场景）
        Set<String> kbIdSet = filteredDocs.stream()
            .map(KbDocument::getKnowledgeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        int docCount = filteredDocs.size();
        log.info("[RAG检索] kbType={} 查询到{}个启用的知识库，{}个文档", kbType, kbIdSet.size(), docCount);

        // ========== 第三步：反向匹配（纯内存计算） ==========
        List<KbDocument> matchedDocs = matchDocsByReverseTag(question, filteredDocs);
        if (!matchedDocs.isEmpty()) {
            return buildMatchResult(matchedDocs, "反向匹配", kbType);
        }

        // ========== 第四步：模糊匹配（纯内存过滤，不走SQL LIKE） ==========
        log.info("[RAG检索] kbType={} 反向匹配未命中，尝试基于内存的模糊匹配", kbType);
        List<KbDocument> fuzzyDocs = matchDocsByFuzzyMatch(question, filteredDocs);
        if (!fuzzyDocs.isEmpty()) {
            return buildMatchResult(fuzzyDocs, "模糊匹配", kbType);
        }

        // ========== 第五步：全量降级 ==========
        log.info("[RAG检索] kbType={} 标签匹配均未命中，降级为全量检索（仅knowledgeId过滤）", kbType);
        String[] allKbIds = kbIdSet.toArray(new String[0]);
        return new MatchResult(null, allKbIds);
    }

    /**
     * 模糊匹配：将 question 拆分为关键词，在内存中检查 tags 是否包含任一关键词
     * <p>
     * 替代原来的 SQL LIKE 查询，纯内存计算，零SQL开销。
     * 关键词拆分策略：按中英文逗号、空格、常见标点符号拆分。
     *
     * @param question 用户问题
     * @param docs     待过滤的文档列表（已按 knowledgeId 过滤）
     * @return 匹配的文档列表（tags 包含任一关键词）
     */
    private List<KbDocument> matchDocsByFuzzyMatch(String question, List<KbDocument> docs) {
        if (docs == null || docs.isEmpty() || question == null || question.trim().isEmpty()) {
            return List.of();
        }

        // 按中英文逗号、空格、常见标点拆分关键词
        String[] keywords = question.split("[,，、\\s]+");

        List<KbDocument> matched = new ArrayList<>();
        Set<String> matchedDocIds = new HashSet<>();

        for (KbDocument doc : docs) {
            if (doc.getTags() == null || doc.getTags().trim().isEmpty()) {
                continue;
            }

            String docTagsLower = doc.getTags().toLowerCase();
            for (String keyword : keywords) {
                String trimmed = keyword.trim().toLowerCase();
                // 关键词至少2个字符才匹配，避免单字误匹配
                if (trimmed.length() >= 2 && docTagsLower.contains(trimmed)) {
                    if (matchedDocIds.add(doc.getId())) {
                        matched.add(doc);
                        log.info("[RAG检索] 模糊匹配命中: keyword='{}' ∈ tags='{}'", keyword, doc.getTags());
                    }
                    break; // 一个关键词匹配即可，避免重复添加
                }
            }
        }
        return matched;
    }

    /**
     * 从匹配的文档中提取 tags 和 knowledgeId，构建 MatchResult
     */
    private MatchResult buildMatchResult(List<KbDocument> docs, String matchType, String kbType) {
        Set<String> tagSet = new LinkedHashSet<>();
        Set<String> kbIdSet = new LinkedHashSet<>();
        for (KbDocument doc : docs) {
            if (doc.getTags() != null && !doc.getTags().isEmpty()) {
                // 把整个 tags 字符串作为过滤值（向量库 metadata 中的完整值）
                tagSet.add(doc.getTags());
            }
            if (doc.getKnowledgeId() != null) {
                kbIdSet.add(doc.getKnowledgeId());
            }
        }
        String[] tagValues = tagSet.toArray(new String[0]);
        String[] knowledgeIds = kbIdSet.toArray(new String[0]);
        log.info("[RAG检索] {}成功，命中{}个文档，{}个tags值，{}个知识库",
            matchType, docs.size(), tagValues.length, knowledgeIds.length);
        return new MatchResult(tagValues, knowledgeIds);
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
        Set<String> matchedDocIds = new HashSet<>(); // 防止重复添加同一文档

        for (KbDocument doc : docs) {
            if (doc.getTags() == null || doc.getTags().trim().isEmpty()) {
                matched.add(doc);
                continue;
            }

            // 按逗号分割 tags（兼容中英文逗号）
            String[] tagArray = doc.getTags().split("[,，]");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                // tag 至少2个字符才匹配，避免单字误匹配
                if (trimmedTag.length() >= 2 && question.contains(trimmedTag)) {
                    // 防止重复添加同一文档
                    if (matchedDocIds.add(doc.getId())) {
                        matched.add(doc);
                        log.info("[RAG检索] 反向匹配命中: tag='{}' ∈ question='{}'", trimmedTag, question);
                    }
                    break; // 一个 tag 匹配即可，避免重复添加
                }
            }
        }
        return matched;
    }

    /**
     * 第二步：向量检索（tags + knowledgeId 双重过滤）
     * <p>
     * 根据是否启用 Reranker 动态调整检索参数：
     * - 启用 Reranker：topK=10, 相似度阈值=0.5（召回更多候选，交给Reranker精排）
     * - 未启用 Reranker：topK=3, 相似度阈值=0.7（直接取高相似度片段）
     */
    private List<Document> vectorSearch(String question, MatchResult matchResult) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        Filter.Expression expression;
        if (matchResult.hasTagValues()) {
            // 双重过滤：tags IN (...) AND knowledgeId IN (...)
            expression = b.and(
                b.in("tags", matchResult.getTagValues()),
                b.in("knowledgeId", matchResult.getKnowledgeIds())
            ).build();
            log.info("[RAG检索] 启用双重过滤：tags IN {} AND knowledgeId IN {}",
                matchResult.getTagValues().length, matchResult.getKnowledgeIds().length);
        } else {
            // 仅 knowledgeId 过滤（降级场景）
            expression = b.in("knowledgeId", matchResult.getKnowledgeIds()).build();
            log.info("[RAG检索] 启用单重过滤：knowledgeId IN {}", matchResult.getKnowledgeIds().length);
        }

        boolean rerankEnabled = rerankerService.isEnabled();
        int retrieveTopK = rerankEnabled ? 10 : TOP_N;
        double similarityThreshold = rerankEnabled ? 0.5 : 0.7;

        SearchRequest request = SearchRequest.builder()
            .query(question)
            .topK(retrieveTopK)
            .similarityThreshold(similarityThreshold)
            .filterExpression(expression)
            .build();

        return knowledgeVectorStore.similaritySearch(request);
    }

    /**
     * 第三步：Reranker 重排序 + 截取 TopN
     */
    private List<Document> rerankAndTrim(String question, List<Document> results) {
        boolean rerankEnabled = rerankerService.isEnabled();
        // 取前 min(TOP_N, results.size()) 条，避免越界
        int trimTo = Math.min(TOP_N, results.size());

        if (rerankEnabled && results.size() > 1) {
            List<String> docTexts = results.stream().map(Document::getText).toList();
            List<Integer> rerankedIndices = rerankerService.rerank(question, docTexts);

            if (!rerankedIndices.isEmpty()) {
                // 按 Reranker 返回的索引顺序组装结果
                List<Document> reranked = new ArrayList<>();
                for (int idx : rerankedIndices) {
                    if (idx < results.size()) {
                        reranked.add(results.get(idx));
                    }
                }
                // 只保留前 TOP_N 条
                if (reranked.size() > TOP_N) {
                    reranked = new ArrayList<>(reranked.subList(0, TOP_N));
                }
                log.info("[RAG检索] 向量检索{}条 → 重排序后取{}条", results.size(), reranked.size());
                return reranked;
            } else {
                log.warn("[RAG检索] Reranker降级，使用向量检索前{}条", trimTo);
            }
        }

        return results.subList(0, trimTo);
    }

    /**
     * 兼容旧调用方式：默认 kbType=10（通用知识）
     */
    public String retrieveContext(String question) {
        return retrieveContext(question, "10");
    }

    /**
     * 匹配结果封装：包含 tags 过滤值和 knowledgeIds
     */
    private static class MatchResult {
        private final String[] tagValues;
        private final String[] knowledgeIds;

        MatchResult(String[] tagValues, String[] knowledgeIds) {
            this.tagValues = tagValues;
            this.knowledgeIds = knowledgeIds != null ? knowledgeIds : new String[0];
        }

        boolean hasTagValues() {
            return tagValues != null && tagValues.length > 0;
        }

        boolean hasKnowledgeIds() {
            return knowledgeIds != null && knowledgeIds.length > 0;
        }

        String[] getTagValues() {
            return tagValues;
        }

        String[] getKnowledgeIds() {
            return knowledgeIds;
        }
    }
}