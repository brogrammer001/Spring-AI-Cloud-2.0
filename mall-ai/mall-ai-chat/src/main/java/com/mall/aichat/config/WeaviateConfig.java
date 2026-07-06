package com.mall.aichat.config;

import io.weaviate.client.WeaviateClient;
import io.weaviate.client.v1.misc.model.VectorIndexConfig;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.weaviate.WeaviateVectorStore;
import org.springframework.ai.vectorstore.weaviate.WeaviateVectorStoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class WeaviateConfig {

    private static final Logger log = LoggerFactory.getLogger(WeaviateConfig.class);
    @Value("${weaviate.knowledge-object-class}")
    private String weaviateKnowledgeObjectClass;

    @Value("${weaviate.chat-memory-object-class}")
    private String weaviateChatMemoryObjectClass;

    /**
     * ① 会话向量库 (用于 ChatMemoryAdvisor 检索对话历史)
     */
    @Bean("conversationVectorStore")
    public VectorStore conversationVectorStore(WeaviateClient weaviateClient, OpenAiEmbeddingModel openAiEmbedding) {
        WeaviateVectorStoreOptions weaviateOptions = new WeaviateVectorStoreOptions();
        weaviateOptions.setObjectClass(weaviateChatMemoryObjectClass);
        return WeaviateVectorStore.builder(weaviateClient, openAiEmbedding)
            .options(weaviateOptions)
            .filterMetadataFields(List.of(
                WeaviateVectorStore.MetadataField.text("conversationId")
            ))
            .build();
    }

    /**
     * ② 知识库向量库 (用于 RAG 检索文档知识)
     */
    @Bean("knowledgeVectorStore")
    public VectorStore knowledgeVectorStore(WeaviateClient weaviateClient, OpenAiEmbeddingModel embeddingModel) {
        WeaviateVectorStoreOptions weaviateOptions = new WeaviateVectorStoreOptions();
        weaviateOptions.setObjectClass(weaviateKnowledgeObjectClass);
        return WeaviateVectorStore.builder(weaviateClient, embeddingModel)
            .options(weaviateOptions)
            .filterMetadataFields(List.of(
                WeaviateVectorStore.MetadataField.text("knowledgeId")
            ))
            .build();
    }

    /**
     * 统一的 Schema 自动初始化器
     */
    @Bean
    public ApplicationRunner initWeaviateSchema(WeaviateClient weaviateClient) {
        WeaviateVectorStoreOptions weaviateOptions = new WeaviateVectorStoreOptions();
        return _ -> {
            // 1. 创建会话历史 Schema
            createSchemaIfNotExists(weaviateClient, weaviateChatMemoryObjectClass, List.of(
                Property.builder().name("metadata").dataType(List.of("text")).build(),
                Property.builder().name(weaviateOptions.getContentFieldName()).dataType(List.of("text")).build(),
                Property.builder().name(weaviateOptions.getMetaFieldPrefix() + "conversationId").dataType(List.of("text")).build()
            ));

            // 2. 创建知识库 Schema
            createSchemaIfNotExists(weaviateClient, weaviateKnowledgeObjectClass, List.of(
                Property.builder().name("metadata").dataType(List.of("text")).build(),
                Property.builder().name(weaviateOptions.getContentFieldName()).dataType(List.of("text")).build(),
                Property.builder().name(weaviateOptions.getMetaFieldPrefix() + "knowledgeId").dataType(List.of("text")).build()
            ));
        };
    }

    /**
     * 辅助方法：检查并创建 Schema
     */
    private void createSchemaIfNotExists(WeaviateClient weaviateClient, String className, List<Property> properties) {
        try {
            Boolean exists = weaviateClient.schema().exists().withClassName(className).run().getResult();
            if (Boolean.TRUE.equals(exists)) {
                log.error("Schema [{}] 已存在，跳过创建。", className);
                return;
            }

            VectorIndexConfig vectorIndexConfig = VectorIndexConfig.builder()
                .distance("cosine")
                .build();

            WeaviateClass weaviateClass = WeaviateClass.builder()
                .className(className)
                .description("Vector Store")
                .vectorizer("none") // 向量由 Spring AI 计算后传入
                .vectorIndexType("hnsw")
                .vectorIndexConfig(vectorIndexConfig)
                .properties(properties)
                .build();

            weaviateClient.schema().classCreator().withClass(weaviateClass).run();
            log.info("Schema [{}] 创建成功。", className);
        } catch (Exception e) {
            log.error("初始化 Schema [{}] 失败: {}", className, e.getMessage());
        }
    }
}
