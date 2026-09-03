package com.mall.aichat.chunker;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档分块策略接口
 * <p>
 * 定义文档分块的标准接口，不同的分块策略实现此接口
 *
 * @author mall
 */
public interface Chunker {

    /**
     * 判断该分块器是否支持当前配置
     *
     * @param semanticEnabled 是否启用语义分块
     * @param chunkSize       分块大小（token 数）
     * @return 是否支持
     */
    boolean supports(boolean semanticEnabled, int chunkSize);

    /**
     * 执行分块操作
     *
     * @param document 输入文档
     * @param chunkSize 分块大小（token 数）
     * @return 分块后的文档列表
     */
    List<Document> chunk(Document document, int chunkSize);
}
