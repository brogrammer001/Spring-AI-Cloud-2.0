package com.mall.aichat.chunker;

import com.knuddels.jtokkit.api.EncodingType;
import com.mall.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Token 分块策略（固定分块，兜底策略）
 * <p>
 * 使用 Spring AI 的 {@link TokenTextSplitter} 进行基于 token 数量的分块，
 * 当未启用语义分块且未配置自定义分隔符时命中。
 *
 * @author mall
 */
@Component
@Order(30)
public class TokenChunker implements Chunker {

    private static final Logger log = LoggerFactory.getLogger(TokenChunker.class);

    @Override
    public boolean supports(boolean semanticEnabled, int chunkSize, String chunkSeparator) {
        // 兜底策略：未启用语义分块且未配置自定义分隔符时命中
        return !semanticEnabled && chunkSize > 0 && StringUtils.isEmpty(chunkSeparator);
    }

    @Override
    public List<Document> chunk(Document document, int chunkSize, String chunkSeparator) {
        log.debug("使用 Token 固定分块，chunkSize={}", chunkSize);
        TokenTextSplitter splitter = buildTokenSplitter(chunkSize);
        return splitter.apply(List.of(document));
    }

    /**
     * 构建统一的 Token 切分器配置
     */
    private TokenTextSplitter buildTokenSplitter(int chunkSize) {
        return TokenTextSplitter.builder()
            .withChunkSize(chunkSize)
            .withMinChunkSizeChars((int) (chunkSize * 0.1))
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            .withPunctuationMarks(List.of('.', '?', '!', '。', '？', '！', '\n', ';', '；'))
            .withEncodingType(EncodingType.CL100K_BASE)
            .build();
    }
}
