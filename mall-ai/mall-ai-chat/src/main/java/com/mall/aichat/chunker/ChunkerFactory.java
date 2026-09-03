package com.mall.aichat.chunker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档分块策略工厂
 * <p>
 * 根据配置自动选择对应的 {@link Chunker} 实现类
 * 使用 Spring 依赖注入自动发现所有 Chunker Bean
 *
 * @author mall
 */
@Component
public class ChunkerFactory {

    private static final Logger log = LoggerFactory.getLogger(ChunkerFactory.class);

    private final List<Chunker> chunkers;

    /**
     * 构造器注入：Spring 会自动注入所有 Chunker Bean
     */
    public ChunkerFactory(List<Chunker> chunkers) {
        this.chunkers = chunkers;
    }

    /**
     * 智能选择最合适的分块策略
     *
     * @param semanticEnabled 是否启用语义分块
     * @param chunkSize       分块大小（token 数）
     * @return 匹配的分块器
     */
    public Chunker selectChunker(boolean semanticEnabled, int chunkSize) {
        log.debug("选择分块策略：semanticEnabled={}, chunkSize={}", semanticEnabled, chunkSize);

        // 按优先级选择：优先选择 supports() 返回 true 的第一个分块器
        // Spring Bean 的加载顺序决定了优先级，建议手动排序确保语义分块优先
        for (Chunker chunker : chunkers) {
            if (chunker.supports(semanticEnabled, chunkSize)) {
                log.info("已选择分块策略：{}", chunker.getClass().getSimpleName());
                return chunker;
            }
        }

        // 默认回退到 Token 分块
        log.warn("未找到匹配的分块策略，降级为 Token 分块");
        return getDefaultChunker();
    }

    /**
     * 执行分块操作
     *
     * @param document 输入文档
     * @param semanticEnabled 是否启用语义分块
     * @param chunkSize 分块大小（token 数）
     * @return 分块后的文档列表
     */
    public List<Document> chunk(Document document, boolean semanticEnabled, int chunkSize) {
        Chunker chunker = selectChunker(semanticEnabled, chunkSize);
        return chunker.chunk(document, chunkSize);
    }

    /**
     * 获取默认分块器（通常是第一个或 TokenChunker）
     */
    private Chunker getDefaultChunker() {
        // 优先返回 TokenChunker 作为默认值
        for (Chunker chunker : chunkers) {
            if (chunker instanceof TokenChunker) {
                return chunker;
            }
        }
        // 如果没有任何分块器，返回第一个
        return chunkers.isEmpty() ? null : chunkers.getFirst();
    }
}
