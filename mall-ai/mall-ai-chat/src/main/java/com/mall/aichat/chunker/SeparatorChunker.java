package com.mall.aichat.chunker;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.mall.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 分隔符分块策略
 * <p>
 * 按用户自定义分隔符（正则，非法正则时按字面量回退）切分文档，块超长时用 {@link TokenTextSplitter} 兜底细切，
 * 块过小时向前合并到相邻块（统一 token 度量），兼顾结构完整性与 Token 上限。
 * 适用于结构规整的文档（Markdown 标题、代码、FAQ、日志等）。
 * <p>
 * 要求 JDK 21+（使用了 SequencedCollection#getLast）。
 *
 * @author mall
 */
@Component
@Order(20)
public class SeparatorChunker implements Chunker {

    private static final Logger log = LoggerFactory.getLogger(SeparatorChunker.class);

    /** 最小语义单元 token 数：低于此值的块尝试向前合并（统一 token 度量，中英文行为一致） */
    private static final int MIN_UNIT_TOKENS = 100;
    /** 合并块时使用的连接符 */
    private static final String MERGE_JOINER = "\n\n";

    /** Token 编码器：与 TokenTextSplitter 的 CL100K_BASE 保持一致（静态缓存，避免每次调用查注册表） */
    private static final Encoding TOKEN_ENCODING = Encodings.newDefaultEncodingRegistry()
        .getEncoding(EncodingType.CL100K_BASE);

    @Override
    public boolean supports(boolean semanticEnabled, int chunkSize, String chunkSeparator) {
        // 未启用语义分块，且用户显式配置了分隔符时命中
        return !semanticEnabled && chunkSize > 0 && StringUtils.isNotEmpty(chunkSeparator);
    }

    @Override
    public List<Document> chunk(Document document, int chunkSize, String chunkSeparator) {
        String text = document.getText();
        if (StringUtils.isEmpty(text)) {
            return List.of();
        }

        // 1. 按自定义分隔符切分为原始块
        List<String> rawChunks = splitBySeparator(text, chunkSeparator);
        if (rawChunks.isEmpty()) {
            log.warn("分隔符 [{}] 未匹配到任何切分点，降级为 Token 分块", chunkSeparator);
            return buildTokenSplitter(chunkSize).apply(List.of(document));
        }

        // 2. 过小的块向前合并（不超过 chunkSize 上限）
        List<String> mergedChunks = mergeSmallChunks(rawChunks, chunkSize);

        // 3. 构建 Document（metadata 每块独立拷贝，避免共享同一 Map 实例被下游串改），超长块用 TokenTextSplitter 细切
        List<Document> result = new ArrayList<>();
        TokenTextSplitter splitter = buildTokenSplitter(chunkSize);
        for (String chunkText : mergedChunks) {
            if (StringUtils.isEmpty(chunkText)) {
                continue;
            }
            if (estimateTokens(chunkText) > chunkSize) {
                Document oversized = Document.builder()
                    .text(chunkText)
                    .metadata(copyMetadata(document))
                    .build();
                result.addAll(splitter.apply(List.of(oversized)));
            } else {
                result.add(Document.builder()
                    .text(chunkText)
                    .metadata(copyMetadata(document))
                    .build());
            }
        }

        log.info("分隔符分块完成：separator=[{}], 原始块 {} 个，最终块 {} 个",
            chunkSeparator, rawChunks.size(), result.size());
        return result;
    }

    /**
     * 每块独立拷贝 metadata：共享同一 Map 实例会被下游对单块的修改串改所有兄弟块
     */
    private Map<String, Object> copyMetadata(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }

    /**
     * 按分隔符切分文本：优先按正则解析，非法正则回退为字面量匹配
     */
    private List<String> splitBySeparator(String text, String separator) {
        List<String> chunks = new ArrayList<>();
        // 使用 -1 limit 保留末尾空串，方便统一过滤
        String[] parts = compileSeparator(separator).split(text, -1);
        for (String part : parts) {
            String trimmed = part.trim();
            if (StringUtils.isNotEmpty(trimmed)) {
                chunks.add(trimmed);
            }
        }
        return chunks;
    }

    /**
     * 编译分隔符：合法正则直接使用；非法正则按字面量处理（Pattern.quote），
     * 避免用户配置含正则特殊字符的字面量分隔符（如 "..."、"|"、"("）时抛异常中断分块
     */
    private Pattern compileSeparator(String separator) {
        try {
            return Pattern.compile(separator);
        } catch (PatternSyntaxException e) {
            log.warn("分隔符 [{}] 不是合法正则，按字面量处理", separator);
            return Pattern.compile(Pattern.quote(separator));
        }
    }

    /**
     * 过小的块向前合并（在不超过 chunkSize 上限的前提下），统一 token 度量
     */
    private List<String> mergeSmallChunks(List<String> chunks, int chunkSize) {
        List<String> merged = new ArrayList<>();
        for (String chunk : chunks) {
            if (!merged.isEmpty()
                && estimateTokens(merged.getLast()) < MIN_UNIT_TOKENS
                && estimateTokens(merged.getLast() + MERGE_JOINER + chunk) <= chunkSize) {
                merged.set(merged.size() - 1, merged.getLast() + MERGE_JOINER + chunk);
            } else {
                merged.add(chunk);
            }
        }
        // 末尾残留的小块向前合并
        if (merged.size() > 1
            && estimateTokens(merged.getLast()) < MIN_UNIT_TOKENS
            && estimateTokens(merged.get(merged.size() - 2) + MERGE_JOINER + merged.getLast()) <= chunkSize) {
            String last = merged.remove(merged.size() - 1);
            merged.set(merged.size() - 1, merged.getLast() + MERGE_JOINER + last);
        }
        return merged;
    }

    /**
     * 构建统一的 Token 切分器（用于超长块兜底细切）
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

    /**
     * 估算文本的 Token 数
     */
    private int estimateTokens(String text) {
        if (StringUtils.isEmpty(text)) {
            return 0;
        }
        return TOKEN_ENCODING.encode(text).size();
    }
}
