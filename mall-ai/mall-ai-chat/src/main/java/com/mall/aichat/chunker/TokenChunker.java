package com.mall.aichat.chunker;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.mall.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Token 分块策略（固定分块）
 * <p>
 * 使用 Spring AI 的 TokenTextSplitter 进行基于 token 数量的分块
 * 适用于不支持语义分块或文档较短的场景
 *
 * @author mall
 */
@Component
public class TokenChunker implements Chunker {

    private static final Logger log = LoggerFactory.getLogger(TokenChunker.class);

    /** Token 编码器：与 TokenTextSplitter 的 CL100K_BASE 保持一致 */
    private static final EncodingRegistry ENCODING_REGISTRY = Encodings.newLazyEncodingRegistry();

    @Override
    public boolean supports(boolean semanticEnabled, int chunkSize) {
        // 仅当未启用语义分块时支持
        return !semanticEnabled && chunkSize > 0;
    }

    @Override
    public List<Document> chunk(Document document, int chunkSize) {
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
