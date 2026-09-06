package com.mall.aichat.chunker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档分块策略工厂
 * <p>
 * 根据配置自动选择对应的 {@link Chunker} 实现类，
 * 使用 Spring 依赖注入自动发现所有 Chunker Bean，按 {@code @Order} 优先级排序。
 * <p>
 * 当前策略优先级：SemanticChunker(10) &gt; SeparatorChunker(20) &gt; TokenChunker(30)
 *
 * @author mall
 */
@Component
public class ChunkerFactory {

    private static final Logger log = LoggerFactory.getLogger(ChunkerFactory.class);

    private final List<Chunker> chunkers;

    /**
     * 构造器注入：Spring 会按 {@code @Order} 顺序注入所有 Chunker Bean
     */
    public ChunkerFactory(List<Chunker> chunkers) {
        this.chunkers = chunkers;
    }

    /**
     * 智能选择最合适的分块策略
     *
     * @param semanticEnabled 是否启用语义分块
     * @param chunkSize       分块大小（token 数）
     * @param chunkSeparator  自定义分隔符（可为空）
     * @return 匹配的分块器
     */
    public Chunker selectChunker(boolean semanticEnabled, int chunkSize, String chunkSeparator) {
        log.debug("选择分块策略：semanticEnabled={}, chunkSize={}, chunkSeparator={}",
            semanticEnabled, chunkSize, chunkSeparator);

        // 按 @Order 优先级依次询问 supports()，命中即返回
        for (Chunker chunker : chunkers) {
            if (chunker.supports(semanticEnabled, chunkSize, chunkSeparator)) {
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
     * @param document        输入文档
     * @param semanticEnabled 是否启用语义分块
     * @param chunkSize       分块大小（token 数）
     * @param chunkSeparator  自定义分隔符（可为空）
     * @return 分块后的文档列表
     */
    public List<Document> chunk(Document document, boolean semanticEnabled, int chunkSize, String chunkSeparator) {
        Chunker chunker = selectChunker(semanticEnabled, chunkSize, chunkSeparator);
        if (chunker == null) {
            log.error("无可用的分块器，返回空列表");
            return List.of();
        }
        return chunker.chunk(document, chunkSize, chunkSeparator);
    }

    /**
     * 获取默认分块器（优先 TokenChunker）
     */
    private Chunker getDefaultChunker() {
        for (Chunker chunker : chunkers) {
            if (chunker instanceof TokenChunker) {
                return chunker;
            }
        }
        return chunkers.isEmpty() ? null : chunkers.getFirst();
    }
}
