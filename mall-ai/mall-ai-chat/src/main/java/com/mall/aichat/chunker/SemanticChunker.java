package com.mall.aichat.chunker;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.mall.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 语义分块策略（多尺度滑动窗口 + 动态阈值）
 * <p>
 * 使用 EmbeddingModel 计算语义相似度，基于多尺度滑动窗口算法进行智能切分
 * 适用于对语义连贯性要求较高的场景
 *
 * @author mall
 */
@Component
public class SemanticChunker implements Chunker {

    private static final Logger log = LoggerFactory.getLogger(SemanticChunker.class);

    // ============ 语义分块参数 ============
    /** 多尺度滑动窗口大小集合 */
    private static final int[] WINDOW_SIZES = {1, 2};
    /** 最大窗口大小 */
    private static final int MAX_WINDOW_SIZE = 2;
    /** 动态切分阈值百分位 */
    private static final double SPLIT_PERCENTILE = 0.20;
    /** 语义分块阈值计算的最小间隔样本数 */
    private static final int MIN_GAP_SAMPLES = 4;
    /** 最小语义单元/语义块字符数 */
    private static final int MIN_UNIT_CHARS = 250;

    /** Token 编码器 */
    private static final EncodingRegistry ENCODING_REGISTRY = Encodings.newLazyEncodingRegistry();

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Override
    public boolean supports(boolean semanticEnabled, int chunkSize) {
        // 启用语义分块且配置了 EmbeddingModel 时支持
        return semanticEnabled && chunkSize > 0 && embeddingModel != null;
    }

    @Override
    public List<Document> chunk(Document document, int chunkSize) {
        String text = document.getText();
        
        // 1. 按段落/句子切分为最小单元
        List<String> units = splitIntoSemanticUnits(text, "\\n\\n", chunkSize);
        
        if (units.size() <= 1) {
            log.debug("语义单元数 {} 不足，降级为 Token 分块", units.size());
            return tokenSplit(document, chunkSize);
        }

        // 2. 单元数不足以构成滑动窗口断点，降级为 Token 分块
        if (units.size() < 2 * MAX_WINDOW_SIZE + 1) {
            log.info("语义单元数 {} 过少，无法构成窗口大小为 {} 的滑动窗口断点，采用 Token 分块", 
                units.size(), MAX_WINDOW_SIZE);
            return tokenSplit(document, chunkSize);
        }

        // 3. 计算每个单元的 embedding 向量
        List<float[]> embeddings = computeEmbeddings(units);
        if (embeddings == null || embeddings.isEmpty()) {
            log.warn("Embedding 计算失败，降级为 Token 分块");
            return tokenSplit(document, chunkSize);
        }

        // 4. 多尺度滑动窗口计算切分点
        List<Integer> splitPoints = findSplitPoints(units, embeddings);
        
        if (splitPoints.size() < MIN_GAP_SAMPLES) {
            log.info("语义断点样本数 {} 不足，降级为 Token 分块", splitPoints.size());
            return tokenSplit(document, chunkSize);
        }

        // 5. 按切分点合并单元形成语义块
        List<String> chunkTexts = mergeUnitsToChunks(units, splitPoints);
        
        // 6. 合并过小的语义块
        chunkTexts = mergeSmallChunks(chunkTexts, chunkSize);
        
        // 7. 构建 Document 列表并按 Token 数细切分超长块
        List<Document> semanticChunks = buildDocuments(chunkTexts, document.getMetadata());
        return fineTuneByToken(semanticChunks, chunkSize);
    }

    /**
     * 计算所有单元的 embedding 向量（分批处理避免单次请求过大）
     */
    private List<float[]> computeEmbeddings(List<String> units) {
        List<float[]> embeddings = new ArrayList<>();
        try {
            int batchSize = 20;
            for (int i = 0; i < units.size(); i += batchSize) {
                int end = Math.min(i + batchSize, units.size());
                List<String> batch = units.subList(i, end);
                List<float[]> batchEmbeddings = embeddingModel.embed(batch);
                embeddings.addAll(batchEmbeddings);
            }
        } catch (Exception e) {
            log.error("Embedding 计算失败", e);
            return null;
        }
        return embeddings;
    }

    /**
     * 多尺度滑动窗口计算切分点
     */
    private List<Integer> findSplitPoints(List<String> units, List<float[]> embeddings) {
        List<Integer> gaps = new ArrayList<>();
        List<Double> gapSimilarities = new ArrayList<>();
        
        for (int p = MAX_WINDOW_SIZE; p <= units.size() - MAX_WINDOW_SIZE; p++) {
            double sum = 0.0;
            for (int w : WINDOW_SIZES) {
                float[] left = averageEmbedding(embeddings.subList(p - w, p));
                float[] right = averageEmbedding(embeddings.subList(p, p + w));
                sum += cosineSimilarity(left, right);
            }
            gaps.add(p);
            gapSimilarities.add(sum / WINDOW_SIZES.length);
        }

        // 动态阈值：取相似度最低的 SPLIT_PERCENTILE 分位
        List<Double> sorted = gapSimilarities.stream().sorted().collect(Collectors.toList());
        int idx = Math.max(0, (int) Math.floor(SPLIT_PERCENTILE * (sorted.size() - 1)));
        double splitThreshold = sorted.get(idx);

        List<Integer> splitPoints = new ArrayList<>();
        for (int g = 0; g < gaps.size(); g++) {
            if (gapSimilarities.get(g) < splitThreshold) {
                splitPoints.add(gaps.get(g));
            }
        }
        
        return splitPoints;
    }

    /**
     * 按切分点合并相邻单元
     */
    private List<String> mergeUnitsToChunks(List<String> units, List<Integer> splitPoints) {
        List<String> chunkTexts = new ArrayList<>();
        int start = 0;
        for (int splitPoint : splitPoints) {
            chunkTexts.add(String.join("\n\n", units.subList(start, splitPoint)).trim());
            start = splitPoint;
        }
        chunkTexts.add(String.join("\n\n", units.subList(start, units.size())).trim());
        return chunkTexts;
    }

    /**
     * 合并过小的语义块
     */
    private List<String> mergeSmallChunks(List<String> chunks, int chunkSize) {
        List<String> merged = new ArrayList<>();
        for (String chunk : chunks) {
            if (StringUtils.isEmpty(chunk)) {
                continue;
            }
            if (!merged.isEmpty() && merged.getLast().length() < MIN_UNIT_CHARS
                && estimateTokens(merged.getLast() + chunk) <= chunkSize) {
                merged.set(merged.size() - 1, merged.getLast() + "\n\n" + chunk);
            } else {
                merged.add(chunk);
            }
        }
        // 末尾残留的小块向前合并
        if (merged.size() > 1 && merged.getLast().length() < MIN_UNIT_CHARS
            && estimateTokens(merged.get(merged.size() - 2) + merged.getLast()) <= chunkSize) {
            String last = merged.remove(merged.size() - 1);
            merged.set(merged.size() - 1, merged.getLast() + "\n\n" + last);
        }
        return merged;
    }

    /**
     * 构建 Document 列表
     */
    private List<Document> buildDocuments(List<String> chunkTexts, java.util.Map<String, Object> metadata) {
        List<Document> documents = new ArrayList<>();
        for (String chunkText : chunkTexts) {
            if (StringUtils.isNotEmpty(chunkText)) {
                documents.add(Document.builder().text(chunkText).metadata(metadata).build());
            }
        }
        return documents;
    }

    /**
     * 按 Token 数细切分超长块
     */
    private List<Document> fineTuneByToken(List<Document> chunks, int chunkSize) {
        List<Document> finalChunks = new ArrayList<>();
        TokenTextSplitter splitter = buildTokenSplitter(chunkSize);
        
        for (Document chunk : chunks) {
            if (estimateTokens(chunk.getText()) > chunkSize) {
                finalChunks.addAll(splitter.apply(List.of(chunk)));
            } else {
                finalChunks.add(chunk);
            }
        }
        
        log.info("语义分块完成：最小单元 {} 个，最终块 {} 个", 
            calculateTotalChars(chunks), finalChunks.size());
        return finalChunks;
    }

    /**
     * Token 分块降级方案
     */
    private List<Document> tokenSplit(Document document, int chunkSize) {
        return buildTokenSplitter(chunkSize).apply(List.of(document));
    }

    /**
     * 将文本切分为语义最小单元
     */
    private List<String> splitIntoSemanticUnits(String text, String customSeparator, int chunkSize) {
        List<String> units = new ArrayList<>();
        
        String separator = StringUtils.isNotEmpty(customSeparator) ? customSeparator : "\\n\\n";
        String[] paragraphs = text.split(separator);
        int maxUnitChars = chunkSize * 2;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (StringUtils.isEmpty(trimmed)) {
                continue;
            }

            if (trimmed.length() > maxUnitChars) {
                units.addAll(splitBySentences(trimmed));
            } else {
                units.add(trimmed);
            }
        }
        return units;
    }

    /**
     * 按句子切分文本（支持中英文标点）
     */
    private List<String> splitBySentences(String text) {
        List<String> sentences = new ArrayList<>();
        Pattern pattern = Pattern.compile("[^。！？!?；;]+[。！？!?；;]?");
        Matcher matcher = pattern.matcher(text);

        StringBuilder current = new StringBuilder();
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (StringUtils.isEmpty(sentence)) {
                continue;
            }
            if (!current.isEmpty() && current.length() + sentence.length() < MIN_UNIT_CHARS) {
                current.append(sentence);
            } else {
                if (!current.isEmpty()) {
                    sentences.add(current.toString());
                }
                current = new StringBuilder(sentence);
            }
        }
        if (!current.isEmpty()) {
            sentences.add(current.toString());
        }

        return sentences;
    }

    /**
     * 构建统一的 Token 切分器
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
        return ENCODING_REGISTRY.getEncoding(EncodingType.CL100K_BASE).encode(text).size();
    }

    /**
     * 计算向量组逐维平均向量
     */
    private float[] averageEmbedding(List<float[]> vectors) {
        if (vectors.size() == 1) {
            return vectors.getFirst();
        }
        int dim = vectors.getFirst().length;
        float[] avg = new float[dim];
        for (float[] vec : vectors) {
            for (int d = 0; d < dim && d < vec.length; d++) {
                avg[d] += vec[d];
            }
        }
        for (int d = 0; d < dim; d++) {
            avg[d] /= vectors.size();
        }
        return avg;
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length || vecA.length == 0) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 计算总字符数用于日志
     */
    private long calculateTotalChars(List<Document> chunks) {
        return chunks.stream().mapToLong(c -> c.getText().length()).sum();
    }
}
