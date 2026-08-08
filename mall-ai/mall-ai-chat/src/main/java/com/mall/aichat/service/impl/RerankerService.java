package com.mall.aichat.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 重排序服务，调用外部Reranker模型对检索结果进行二次排序
 */
@Service
public class RerankerService {

    private static final Logger log = LoggerFactory.getLogger(RerankerService.class);

    @Value("${reranker.base-url:http://127.0.0.1:8887}")
    private String baseUrl;

    @Value("${reranker.top-n:3}")
    private int topN;

    @Value("${reranker.enabled:true}")
    private boolean enabled;

    /**
     * 对文档列表进行重排序
     *
     * @param query     用户查询
     * @param documents 待排序的文档文本列表
     * @return 按相关性降序排列的文档索引（仅返回topN个）
     */
    public List<Integer> rerank(String query, List<String> documents) {
        if (!enabled || documents == null || documents.isEmpty()) {
            return List.of();
        }

        try {
            RerankRequest request = new RerankRequest(query, documents, topN);
            RerankResponse response = RestClient.create(baseUrl)
                    .post()
                    .uri("/v1/rerank")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RerankResponse.class);

            if (response == null || response.results() == null) {
                log.warn("Reranker返回空结果，跳过重排序");
                return List.of();
            }

            return response.results().stream()
                    .map(RerankResult::index)
                    .toList();
        } catch (Exception e) {
            log.error("Reranker调用失败，降级使用原始排序: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // --- API 请求/响应 DTO ---

    record RerankRequest(
            String query,
            List<String> documents,
            @JsonProperty("top_n") int topN
    ) {}

    record RerankResponse(List<RerankResult> results) {}

    record RerankResult(
            int index,
            @JsonProperty("relevance_score") double relevanceScore
    ) {}
}
