package com.mall.aichat.config;

import com.mall.aichat.domain.KbKnowledgeBase;
import com.mall.aichat.service.IKbKnowledgeBaseService;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 检索向量知识库库配置
 */
@Component
public class RagConfig {

    @Resource(name = "knowledgeVectorStore")
    private VectorStore vectorStore;

    @Autowired
    private IKbKnowledgeBaseService kbKnowledgeBaseService;

    /**
     * 根据问题从向量库检索相关文档片段
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

        SearchRequest request = SearchRequest.builder()
            .query(question)
            .topK(3)
            .similarityThreshold(0.7)
            .filterExpression(expression)
            .build();

        // 执行向量检索
        List<Document> results = vectorStore.similaritySearch(request);

        if (results.isEmpty()) {
            return "";
        }

        // 拼接检索到的上下文文本
        return results.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n---\n\n"));
    }
}
