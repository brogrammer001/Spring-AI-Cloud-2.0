package com.mall.aichat.config;

import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.misc.model.VectorIndexConfig;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * weaviate向量库创建 scheam
 */
@Configuration
public class WeaviateSchemaInitConfig {

    @Value("${spring.ai.vectorstore.weaviate.object-class}")
    private String weaviateObjectClass;

    @Bean
    public ApplicationRunner schemaInit(WeaviateClient weaviateClient) {
        return args -> {

            // 1. 检查 class 是否已存在
            Result<Boolean> existsResult = weaviateClient.schema()
                .exists()
                .withClassName(weaviateObjectClass)
                .run();

            if (existsResult.hasErrors()) {
                System.err.println("检查 schema 失败: " + existsResult.getError());
                return;
            }

            Boolean exists = existsResult.getResult();
            if (Boolean.TRUE.equals(exists)) {
                System.out.println("Schema '" + weaviateObjectClass + "' 已存在，跳过创建。");
                return;
            }

            // 2. 不存在则构建 schema（等价于 curl 的 JSON）
            VectorIndexConfig vectorIndexConfig = VectorIndexConfig.builder()
                .distance("cosine")
                .build();

            WeaviateClass weaviateClass = WeaviateClass.builder()
                .className(weaviateObjectClass)
                .description("Spring AI vector store")
                .vectorizer("none")          // 向量由 Spring AI 端计算后传入
                .vectorIndexType("hnsw")
                .vectorIndexConfig(vectorIndexConfig)
                .properties(List.of(
                    Property.builder()
                        .name("content")
                        .dataType(List.of("text"))
                        .build(),
                    Property.builder()
                        .name("metadata")
                        .dataType(List.of("text"))
                        .build(),
                    // 注意大小写：与 Spring AI 查询时生成的字段名保持一致
                    Property.builder()
                        .name("meta_conversationId")
                        .dataType(List.of("text"))
                        .build()
                ))
                .build();

            // 3. 创建 schema
            Result<Boolean> createResult = weaviateClient.schema()
                .classCreator()
                .withClass(weaviateClass)
                .run();

            if (createResult.hasErrors()) {
                System.err.println("创建 schema 失败: " + createResult.getError());
            } else {
                System.out.println("Schema '" + weaviateObjectClass + "' 创建成功。");
            }
        };
    }
}
