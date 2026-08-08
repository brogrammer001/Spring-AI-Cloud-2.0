package com.mall.aichat.service.impl;

import com.mall.aichat.domain.KbKnowledgeBase;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 检索向量知识库库配置
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
    private RerankerService rerankerService;

    /**
     * 根据问题从向量库检索相关文档片段
     * 流程：向量检索（topK=10）→ Reranker重排序 → 取top3
     */
    public String retrieveContext(String question) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        KbKnowledgeBase kbKnowledgeBase = new KbKnowledgeBase();
        kbKnowledgeBase.setStatus("0");
        List<KbKnowledgeBase> kbKnowledgeBases = kbKnowledgeBaseService.selectKbKnowledgeBaseList(kbKnowledgeBase);

        if (kbKnowledgeBases.isEmpty()) {
            return "";
        }

        String[] knowledgeIds = kbKnowledgeBases.stream().map(KbKnowledgeBase::getId).toArray(String[]::new);

        Filter.Expression expression = b.in("knowledgeId", knowledgeIds).build();

        // 向量检索：多取一些用于重排序
        int retrieveTopK = rerankerService.isEnabled() ? 10 : 3;
        SearchRequest request = SearchRequest.builder()
            .query(question)
            .topK(retrieveTopK)
            .similarityThreshold(0.5)
            .filterExpression(expression)
            .build();

        // 执行向量检索
        List<Document> results = knowledgeVectorStore.similaritySearch(request);

        if (results.isEmpty()) {
            return "";
        }

        // Reranker重排序
        List<Document> finalResults;
        if (rerankerService.isEnabled() && results.size() > 1) {
            List<String> docTexts = results.stream().map(Document::getText).toList();
            List<Integer> rerankedIndices = rerankerService.rerank(question, docTexts);

            if (!rerankedIndices.isEmpty()) {
                finalResults = new ArrayList<>();
                for (int idx : rerankedIndices) {
                    if (idx < results.size()) {
                        finalResults.add(results.get(idx));
                    }
                }
                log.info("RAG检索：向量检索{}条 → 重排序后取{}条", results.size(), finalResults.size());
            } else {
                // reranker降级，取前3条
                finalResults = results.subList(0, Math.min(3, results.size()));
                log.warn("Reranker降级，使用向量检索前{}条", finalResults.size());
            }
        } else {
            finalResults = results.subList(0, Math.min(3, results.size()));
        }

        // 拼接检索到的上下文文本
        return finalResults.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n---\n\n"));
    }
}
