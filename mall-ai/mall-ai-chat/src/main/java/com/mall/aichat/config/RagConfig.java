package com.mall.aichat.config;

import org.springframework.context.annotation.Configuration;

//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;

@Configuration
public class RagConfig {

//    private final VectorStore vectorStore;
//
//    public RagConfig(VectorStore vectorStore) {
//        this.vectorStore = vectorStore;
//    }
//
//    /**
//     * 根据问题从向量库检索相关文档片段
//     */
//    public List<String> retrieveContext(String question, int topK) {
//        SearchRequest searchRequest = SearchRequest.builder()
//            .query(question)
//            .topK(topK)
//            .similarityThreshold(0.5)
//            .build();
//
//        return vectorStore.similaritySearch(searchRequest)
//            .stream()
//            .map(Document::getText)
//            .toList();
//    }
}
