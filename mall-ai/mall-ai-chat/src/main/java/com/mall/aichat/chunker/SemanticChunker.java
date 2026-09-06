package com.mall.aichat.chunker;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import com.mall.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * 语义分块策略 V5（多尺度滑动窗口 + 多策略动态阈值 + 父子分块 + 块间重叠）
 * <p>
 * 核心特性：
 * 1. 父子结构：子块用于向量检索，父块通过 metadata(parent_text) 回溯，兼顾检索精度与上下文完整（对齐 Dify 父子模式）；
 *    parent_id 携带文档唯一标识，跨文档全局唯一，检索端可安全按 parent_id 去重；
 *    标题拼入父块文本，保证 parent_text 上下文完整，且子块 embedding 能覆盖标题式 query
 * 2. 多策略动态阈值：percentile / standard_deviation / interquartile / gradient（对齐 LangChain SemanticChunker），
 *    默认 standard_deviation，并叠加局部极小值校验，剔除相似度窄幅抖动造成的噪声切分点；
 *    PERCENTILE 使用独立参数 PERCENTILE_FRACTION（0~1），与 THRESHOLD_AMOUNT 互不干扰
 * 3. 语义断点少不再整体降级为 Token 分块（断点少 = 主题集中，是正常现象）
 * 4. 结构感知切分：优先按 Markdown/中文标题切分，标题作为 metadata 随块携带；
 *    startIndex 通过游标递进定位（O(n)），重复段落也能正确定位
 * 5. 块间重叠：父块间约 12% 句子级重叠，子块间约 15% 重叠，缓解边界误切；
 *    重叠始终取自原始文本快照，不会级联累积
 * 6. 统一 token 度量：最小单元/合并判定全部基于 token，不再混用字符数与 token 数
 * 7. embedding 输入保护：超长单元优先按句子粒度打包切分（语义边界完整），
 *    仅当单句本身超限时才退回 token 窗口硬切，并修复多字节字符断裂造成的 U+FFFD 乱码；
 *    尾片合并前校验合并结果不超模型硬上限，避免触发服务端校验导致整篇文档降级；
 *    返回数量不符时显式报错而非在平均向量处抛 AIOOBE
 * 8. 文本清洗：统一换行符（CRLF/CR → LF，Windows 文档兼容）、替换连续空白、
 *    可选删除 URL/邮箱等对 embedding 相似度有污染的噪声
 * 9. 自定义分隔符：非法正则回退为字面量匹配（与 SeparatorChunker 行为一致），
 *    避免用户配置含正则特殊字符的分隔符时抛异常中断整个语义分块
 * 10. token 计算带 LRU 缓存（短文本），避免分块过程中对相同文本反复 encode
 * <p>
 * 要求 JDK 21+（使用了 SequencedCollection#getFirst/getLast）。
 * 检索端需配合：命中子块后取 metadata.parent_text 作为 LLM 上下文，并按 parent_id 去重。
 * 大规模场景建议改为：父块全文单独建表存储，子块仅保留 parent_id 引用，检索后回查，
 * 以避免父块全文在每个子块 metadata 中重复存储造成的向量库膨胀。
 *
 * @author mall
 */
@Component
@Order(10)
public class SemanticChunker implements Chunker {

    private static final Logger log = LoggerFactory.getLogger(SemanticChunker.class);

    // ============ 滑动窗口参数 ============
    /** 最大滑动窗口大小（实际窗口会按单元数自适应收缩，不再因单元少而放弃语义切分） */
    private static final int MAX_WINDOW_SIZE = 2;

    // ============ 动态阈值参数 ============
    /**
     * 断点阈值策略：
     * PERCENTILE         - 相似度分位数切分（使用 PERCENTILE_FRACTION 参数）。
     *                      对相似度区间极窄的文档易产生噪声切分
     * STANDARD_DEVIATION - 均值 - k*标准差（THRESHOLD_AMOUNT 表示 k，如 1.5，值越大切分越保守）。通用推荐
     * INTERQUARTILE      - Q1 - k*IQR（THRESHOLD_AMOUNT 表示 k，如 1.5）。对离群断点更鲁棒
     * GRADIENT           - 仅在相似度骤降处切分（取梯度前 5%），适合结构清晰的文档
     */
    private enum ThresholdStrategy { PERCENTILE, STANDARD_DEVIATION, INTERQUARTILE, GRADIENT }

    private static final ThresholdStrategy THRESHOLD_STRATEGY = ThresholdStrategy.STANDARD_DEVIATION;
    /** 阈值系数，含义随策略变化：STANDARD_DEVIATION/INTERQUARTILE 为 k 值；PERCENTILE 不使用此参数 */
    private static final double THRESHOLD_AMOUNT = 1.5;
    /** PERCENTILE 策略专用分位数参数（0~1，值越小切分越细）。
     *  不能复用 THRESHOLD_AMOUNT：1.5 会钳位到最大相似度，严格小于判定导致静默零切分 */
    private static final double PERCENTILE_FRACTION = 0.20;
    /** 局部极小值校验半径：断点必须是前后 N 个候选点中的最低点 */
    private static final int LOCAL_MIN_RADIUS = 1;
    private static final double LOCAL_MIN_EPSILON = 1e-6;

    // ============ 父子分块参数 ============
    /** 子块大小 = chunkSize * CHILD_SIZE_RATIO（对齐 Dify：父块给上下文、子块做检索） */
    private static final double CHILD_SIZE_RATIO = 1.0 / 3.0;
    private static final int MIN_CHILD_TOKENS = 64;
    /** 子块间重叠比例 */
    private static final double CHILD_OVERLAP_RATIO = 0.15;

    // ============ 父块间重叠参数 ============
    private static final double PARENT_OVERLAP_RATIO = 0.12;
    private static final int PARENT_OVERLAP_MIN_TOKENS = 30;
    private static final int PARENT_OVERLAP_MAX_TOKENS = 200;

    // ============ 最小单元参数（统一 token 度量） ============
    /** 语义最小单元/最小父块的 token 下限（约等于 250 中文字符） */
    private static final int MIN_UNIT_TOKENS = 100;
    /** 单个语义单元的 token 上限系数：maxUnitTokens = chunkSize * MAX_UNIT_TOKENS_FACTOR */
    private static final int MAX_UNIT_TOKENS_FACTOR = 2;

    // ============ Embedding 参数 ============
    private static final int EMBEDDING_BATCH_SIZE = 16;
    /** 尾片低于该比例时尝试并入前片（合并结果必须仍不超模型硬上限，否则单独 embed 尾片） */
    private static final double MIN_TAIL_RATIO = 0.1;

    // ============ 文本清洗 ============
    /** 对齐 Dify 预处理建议：删除 URL/邮箱。若业务文档 URL 本身是关键信息请置为 false */
    private static final boolean REMOVE_URL_AND_EMAIL = true;
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)https?://\\S+|www\\.\\S+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)[\\w.+-]+@[\\w-]+\\.[\\w.]+");

    // ============ 结构识别 ============
    /** 标题识别：Markdown 标题 / 中文方括号标题 / 中文章节 / 多级编号（如 2.1，刻意排除普通列表项） */
    private static final Pattern HEADING_PATTERN = Pattern.compile(
        "(#{1,6}\\s+\\S.*"
            + "|【[^】]{1,50}】\\s*\\S.*"
            + "|第[一二三四五六七八九十百千0-9]+[章节篇部分卷]\\s*\\S.*"
            + "|\\d+(?:\\.\\d+)+\\s+\\S.*)");
    private static final int MAX_HEADING_LENGTH = 100;
    /** 句子切分（含尾部空白，保证拼接后保留原文间距） */
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^。！？!?；;]+[。！？!?；;]?\\s*");

    // ============ Token 缓存 ============
    /** token 计算缓存：分块过程中同一短文本会被多次度量（分组/合并/重叠等），LRU 避免重复 encode */
    private static final int TOKEN_CACHE_MAX_ENTRIES = 10_000;
    /** 超过该字符长度的文本不入缓存：LRU 容量有限，大字符串会挤占缓存并增加内存压力 */
    private static final int TOKEN_CACHE_MAX_TEXT_LENGTH = 8192;
    private static final Map<String, Integer> TOKEN_CACHE = Collections.synchronizedMap(
        new LinkedHashMap<>(1024, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > TOKEN_CACHE_MAX_ENTRIES;
            }
        });

    /** metadata 中文档唯一标识的 key，用于构建全局唯一的 parent_id */
    private static final String DOC_ID_METADATA_KEY = "doc_id";

    /** Token 编码器：与 TokenTextSplitter 的 CL100K_BASE 保持一致（静态缓存，避免重复查找注册表） */
    private static final Encoding TOKEN_ENCODING = Encodings.newDefaultEncodingRegistry()
        .getEncoding(EncodingType.CL100K_BASE);

    /**
     * embedding 模型单次输入 token 上限，按实际模型配置：
     * text-embedding-3-small=8191、bge-m3=8192、bge-large-zh=512、通用兜底 512
     */
    @Value("${aichat.embedding.max-input-tokens:512}")
    private int embeddingMaxInputTokens;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Override
    public boolean supports(boolean semanticEnabled, int chunkSize, String chunkSeparator) {
        return semanticEnabled && chunkSize > 0 && embeddingModel != null;
    }

    @Override
    public List<Document> chunk(Document document, int chunkSize, String chunkSeparator) {
        // 0. 文本清洗：统一换行符 + 连续空白/URL/邮箱等噪声（噪声会污染 embedding 相似度）
        String text = cleanText(document.getText());
        if (StringUtils.isEmpty(text)) {
            return tokenSplit(document, chunkSize);
        }

        // 1. 结构感知切分：标题 > 段落 > 句子，得到带标题上下文的最小单元（startIndex 游标递进定位）
        List<SemanticUnit> units = splitIntoSemanticUnits(text, chunkSeparator, chunkSize);
        if (units.size() <= 1) {
            log.debug("语义单元数 {} 不足，降级为 Token 分块", units.size());
            return tokenSplit(document, chunkSize);
        }

        // 2. 窗口自适应：单元较少时收缩窗口继续做语义切分，而不是整体放弃
        int windowSize = Math.min(MAX_WINDOW_SIZE, (units.size() - 1) / 2);
        if (windowSize < 1) {
            return tokenSplit(document, chunkSize);
        }

        // 3. 计算单元向量（超长单元按句子粒度预切分，避免超出 embedding 模型输入上限）
        List<float[]> embeddings = computeEmbeddings(units);
        if (embeddings == null || embeddings.isEmpty()) {
            log.warn("Embedding 计算失败，降级为 Token 分块");
            return tokenSplit(document, chunkSize);
        }

        // 4. 多尺度滑动窗口 + 多策略动态阈值 + 局部极小值校验
        List<Integer> splitPoints = findSplitPoints(units, embeddings, windowSize);
        // 注意：断点少说明文档主题集中，属于正常结果，不再因此整体降级为 Token 分块

        // 5. 按切分点分组 -> 组内超长二次拆分 -> 过小组就近双向合并（统一 token 度量）
        List<List<SemanticUnit>> groups = groupUnitsBySplitPoints(units, splitPoints);
        splitOversizedGroups(groups, chunkSize * MAX_UNIT_TOKENS_FACTOR);
        mergeSmallGroups(groups, chunkSize);

        // 6. 物化为父块（标题拼入父块文本，保证上下文完整与标题式 query 召回）
        List<ParentChunk> parents = groups.stream().map(this::toParentChunk).collect(Collectors.toList());

        // 7. 相邻父块注入约 12% 句子级重叠（取自原始文本快照，不级联累积）
        addOverlapBetweenParents(parents);

        // 8. 构建父子结构：返回子块用于向量检索，父块通过 metadata 回溯（parent_id 全局唯一）
        List<Document> children = buildParentChildDocuments(
            parents, document.getMetadata(), chunkSize, resolveDocId(document));

        log.info("语义分块完成：语义单元 {} 个，切分点 {} 个，父块 {} 个，子块 {} 个",
            units.size(), splitPoints.size(), parents.size(), children.size());
        return children;
    }

    // ==================================================================
    // 数据结构
    // ==================================================================

    /**
     * 语义最小单元：文本 + 所属标题 + 原文起始偏移
     */
    private record SemanticUnit(String text, String heading, int startIndex) {
    }

    /**
     * 父块：语义完整的较大单元（含标题行），携带标题与原文位置
     */
    private record ParentChunk(String text, String heading, int startIndex) {
    }

    // ==================================================================
    // 主流程步骤 1：结构感知切分
    // ==================================================================

    /**
     * 结构感知切分：先按标题分成 section，section 内按分隔符分段落，超长段落按句子聚合成 token 受限的单元。
     * startIndex 通过游标递进定位（O(n) 总开销），重复段落按出现顺序各自定位到正确位置；
     * 分隔符非法正则时回退为字面量匹配（与 SeparatorChunker 行为一致）
     */
    private List<SemanticUnit> splitIntoSemanticUnits(String text, String customSeparator, int chunkSize) {
        List<SemanticUnit> units = new ArrayList<>();
        int maxUnitTokens = chunkSize * MAX_UNIT_TOKENS_FACTOR;
        Pattern separator = compileSeparator(
            StringUtils.isNotEmpty(customSeparator) ? customSeparator : "\\n\\n");

        for (String[] section : splitByHeadings(text)) {
            String heading = section[0];
            for (String para : separator.split(section[1])) {
                String trimmed = para.trim();
                if (StringUtils.isEmpty(trimmed)) {
                    continue;
                }
                if (estimateTokens(trimmed) > maxUnitTokens) {
                    for (String group : splitBySentences(trimmed)) {
                        units.add(new SemanticUnit(group, heading, locateForward(text, group, units)));
                    }
                } else {
                    units.add(new SemanticUnit(trimmed, heading, locateForward(text, trimmed, units)));
                }
            }
        }
        return units;
    }

    /**
     * 编译分隔符：合法正则直接使用；非法正则按字面量处理（Pattern.quote），
     * 避免用户配置含正则特殊字符的分隔符（如 "..."、"|"、"("）时抛异常中断整个语义分块
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
     * 从上一个单元的结束位置开始向后定位（游标递进，避免全文 indexOf 的 O(n²) 开销与重复段落误定位）
     */
    private int locateForward(String text, String target, List<SemanticUnit> units) {
        int fromIndex = units.isEmpty() ? 0 : units.getLast().startIndex();
        int idx = text.indexOf(target, Math.max(0, fromIndex));
        return idx >= 0 ? idx : text.indexOf(target);
    }

    /**
     * 按标题行将全文分成若干 section，返回 [标题, 正文] 列表
     */
    private List<String[]> splitByHeadings(String text) {
        List<String[]> sections = new ArrayList<>();
        String currentHeading = "";
        StringBuilder body = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (isHeading(line)) {
                if (body.length() > 0) {
                    sections.add(new String[]{currentHeading, body.toString()});
                    body.setLength(0);
                }
                currentHeading = line.trim();
            } else {
                body.append(line).append('\n');
            }
        }
        if (body.length() > 0 || sections.isEmpty()) {
            sections.add(new String[]{currentHeading, body.toString()});
        }
        return sections;
    }

    private boolean isHeading(String line) {
        String trimmed = line.trim();
        return trimmed.length() <= MAX_HEADING_LENGTH && HEADING_PATTERN.matcher(trimmed).matches();
    }

    /**
     * 按句子聚合成不低于 MIN_UNIT_TOKENS 的文本组
     */
    private List<String> splitBySentences(String text) {
        List<String> groups = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (sentence.isEmpty()) {
                continue;
            }
            if (current.length() > 0 && estimateTokens(current + sentence) < MIN_UNIT_TOKENS) {
                current.append(sentence);
            } else {
                if (current.length() > 0) {
                    groups.add(current.toString().trim());
                }
                current = new StringBuilder(sentence);
            }
        }
        if (current.length() > 0) {
            groups.add(current.toString().trim());
        }
        return groups;
    }

    // ==================================================================
    // 主流程步骤 3：Embedding 计算
    // ==================================================================

    /**
     * 计算所有单元向量：超长单元预切分 -> 分批请求 -> 多片段按维平均
     */
    private List<float[]> computeEmbeddings(List<SemanticUnit> units) {
        try {
            // 1) 预切分，记录每个单元对应的片段数
            List<String> pieces = new ArrayList<>();
            List<Integer> pieceCounts = new ArrayList<>();
            for (SemanticUnit unit : units) {
                List<String> sub = splitForEmbedding(unit.text());
                pieceCounts.add(sub.size());
                pieces.addAll(sub);
            }
            // 2) 分批请求，避免单次请求过大
            List<float[]> pieceVectors = new ArrayList<>(pieces.size());
            for (int i = 0; i < pieces.size(); i += EMBEDDING_BATCH_SIZE) {
                int end = Math.min(i + EMBEDDING_BATCH_SIZE, pieces.size());
                pieceVectors.addAll(embeddingModel.embed(pieces.subList(i, end)));
            }
            // 校验返回数量：服务端异常截断时显式报错并降级，而非在平均向量处抛 AIOOBE 掩盖根因
            if (pieceVectors.size() != pieces.size()) {
                log.error("Embedding 返回数量不符：请求 {} 个片段，返回 {} 个向量",
                    pieces.size(), pieceVectors.size());
                return null;
            }
            // 3) 多片段单元按维平均，得到整单元向量
            List<float[]> embeddings = new ArrayList<>(units.size());
            int cursor = 0;
            for (Integer count : pieceCounts) {
                embeddings.add(averageEmbedding(pieceVectors.subList(cursor, cursor + count)));
                cursor += count;
            }
            return embeddings;
        } catch (Exception e) {
            log.error("Embedding 计算失败", e);
            return null;
        }
    }

    /**
     * 将超长文本切分为不超过 embedding 模型输入上限的分片。
     * 优先按句子粒度打包（语义边界完整）；
     * 仅当单句本身超限时才退回 token 窗口硬切，并修复多字节字符断裂造成的乱码；
     * 尾片合并前校验合并结果仍不超硬上限，避免触发 embedding 服务端校验导致整体失败。
     */
    private List<String> splitForEmbedding(String text) {
        if (estimateTokens(text) <= embeddingMaxInputTokens) {
            return List.of(text);
        }
        List<String> sentences = splitToSentencesKeepText(text);
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;

        for (String sentence : sentences) {
            int st = estimateTokens(sentence);
            if (st > embeddingMaxInputTokens) {
                // 单句本身超限：先落盘已累积内容，再对该句退化为 token 硬切
                if (current.length() > 0) {
                    pieces.add(current.toString().trim());
                    current.setLength(0);
                    currentTokens = 0;
                }
                pieces.addAll(splitByTokenWindowSafe(sentence));
                continue;
            }
            if (currentTokens + st > embeddingMaxInputTokens && current.length() > 0) {
                pieces.add(current.toString().trim());
                current.setLength(0);
                currentTokens = 0;
            }
            current.append(sentence);
            currentTokens += st;
        }
        if (current.length() > 0) {
            String tail = current.toString().trim();
            if (!pieces.isEmpty() && estimateTokens(tail) < embeddingMaxInputTokens * MIN_TAIL_RATIO) {
                String merged = pieces.getLast() + tail;
                // 仅当合并后仍不超硬上限时才合并；否则宁可单独 embed 尾片，
                // 避免触发服务端校验导致 embed 调用失败 -> 整篇文档降级为 token 分块
                if (estimateTokens(merged) <= embeddingMaxInputTokens) {
                    pieces.set(pieces.size() - 1, merged);
                } else {
                    pieces.add(tail);
                }
            } else {
                pieces.add(tail);
            }
        }
        return pieces.isEmpty() ? List.of(text) : pieces;
    }

    /**
     * 保留原文间距的句子切分（与父/子块切分共用同一正则，保证边界一致）
     */
    private List<String> splitToSentencesKeepText(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            if (!matcher.group().trim().isEmpty()) {
                sentences.add(matcher.group());
            }
        }
        return sentences.isEmpty() ? List.of(text) : sentences;
    }

    /**
     * token 窗口硬切兜底，修复切点落在多字节字符中间导致的 U+FFFD 乱码。
     * cl100k 是 byte-level BPE：一个中文字符（3 字节）可能被拆成 2 个 token，
     * 任意下标处硬切 + decode 会产生替换符，必须剔除后再送 embed。
     */
    private List<String> splitByTokenWindowSafe(String text) {
        IntArrayList tokens = TOKEN_ENCODING.encode(text);
        List<String> pieces = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i += embeddingMaxInputTokens) {
            int end = Math.min(i + embeddingMaxInputTokens, tokens.size());
            boolean cutStart = i > 0;
            boolean cutEnd = end < tokens.size();
            // 手工拷贝 token 切片，兼容无 subList 方法的 IntArrayList 版本
            IntArrayList slice = new IntArrayList();
            for (int j = i; j < end; j++) {
                slice.add(tokens.get(j));
            }
            String piece = TOKEN_ENCODING.decode(slice);
            piece = trimReplacementChars(piece, cutStart, cutEnd);
            if (StringUtils.isNotEmpty(piece)) {
                pieces.add(piece);
            }
        }
        return pieces;
    }

    /**
     * 剔除首尾的替换符 U+FFFD（jtokkit 对残缺字节序列 decode 时产生）
     */
    private String trimReplacementChars(String piece, boolean trimStart, boolean trimEnd) {
        int start = 0;
        int end = piece.length();
        if (trimStart) {
            while (start < end && piece.charAt(start) == '\uFFFD') {
                start++;
            }
        }
        if (trimEnd) {
            while (end > start && piece.charAt(end - 1) == '\uFFFD') {
                end--;
            }
        }
        return start == 0 && end == piece.length() ? piece : piece.substring(start, end);
    }

    // ==================================================================
    // 主流程步骤 4：切分点识别
    // ==================================================================

    /**
     * 多尺度滑动窗口计算相邻相似度，再按策略选出切分点
     */
    private List<Integer> findSplitPoints(List<SemanticUnit> units, List<float[]> embeddings, int windowSize) {
        List<Integer> gaps = new ArrayList<>();
        List<Double> similarities = new ArrayList<>();
        for (int p = windowSize; p <= units.size() - windowSize; p++) {
            double sum = 0.0;
            for (int w = 1; w <= windowSize; w++) {
                float[] left = averageEmbedding(embeddings.subList(p - w, p));
                float[] right = averageEmbedding(embeddings.subList(p, p + w));
                sum += cosineSimilarity(left, right);
            }
            gaps.add(p);
            similarities.add(sum / windowSize);
        }
        if (gaps.isEmpty()) {
            return gaps;
        }
        List<Integer> splitPoints = selectSplitPoints(gaps, similarities);
        log.debug("候选断点 {} 个，策略 {}，最终切分点 {} 个", gaps.size(), THRESHOLD_STRATEGY, splitPoints.size());
        return splitPoints;
    }

    /**
     * 按策略选出候选断点，再叠加局部极小值校验。
     * 注意各策略参数含义不同：PERCENTILE 用 PERCENTILE_FRACTION（0~1），
     * STANDARD_DEVIATION/INTERQUARTILE 用 THRESHOLD_AMOUNT（k 值），互不复用
     */
    private List<Integer> selectSplitPoints(List<Integer> gaps, List<Double> similarities) {
        int n = similarities.size();
        List<Integer> candidateIdx = new ArrayList<>();
        switch (THRESHOLD_STRATEGY) {
            case PERCENTILE: {
                List<Double> sorted = similarities.stream().sorted().collect(Collectors.toList());
                double threshold = percentile(sorted, PERCENTILE_FRACTION);
                for (int i = 0; i < n; i++) {
                    if (similarities.get(i) < threshold) {
                        candidateIdx.add(i);
                    }
                }
                break;
            }
            case STANDARD_DEVIATION: {
                double mean = similarities.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
                double std = Math.sqrt(similarities.stream()
                    .mapToDouble(s -> (s - mean) * (s - mean)).average().orElse(0.0));
                double threshold = mean - THRESHOLD_AMOUNT * std;
                for (int i = 0; i < n; i++) {
                    if (similarities.get(i) < threshold) {
                        candidateIdx.add(i);
                    }
                }
                break;
            }
            case INTERQUARTILE: {
                List<Double> sorted = similarities.stream().sorted().collect(Collectors.toList());
                double q1 = percentile(sorted, 0.25);
                double q3 = percentile(sorted, 0.75);
                double threshold = q1 - THRESHOLD_AMOUNT * (q3 - q1);
                for (int i = 0; i < n; i++) {
                    if (similarities.get(i) < threshold) {
                        candidateIdx.add(i);
                    }
                }
                break;
            }
            case GRADIENT: {
                if (n < 3) {
                    break;
                }
                List<Double> drops = new ArrayList<>();
                for (int i = 0; i < n - 1; i++) {
                    drops.add(similarities.get(i) - similarities.get(i + 1));
                }
                List<Double> sortedDrops = drops.stream().sorted().collect(Collectors.toList());
                double dropThreshold = percentile(sortedDrops, 0.95);
                for (int i = 0; i < drops.size(); i++) {
                    // 骤降发生在 i -> i+1 之间，断点落在下降之后的 i+1
                    if (drops.get(i) >= dropThreshold) {
                        candidateIdx.add(i + 1);
                    }
                }
                break;
            }
            default:
                break;
        }
        // 局部极小值校验：断点必须是附近最低点，剔除相似度窄幅抖动造成的噪声切分
        return candidateIdx.stream()
            .filter(i -> i < n && isLocalMinimum(similarities, i))
            .distinct()
            .sorted()
            .map(gaps::get)
            .collect(Collectors.toList());
    }

    private boolean isLocalMinimum(List<Double> similarities, int i) {
        double v = similarities.get(i);
        for (int j = Math.max(0, i - LOCAL_MIN_RADIUS); j <= Math.min(similarities.size() - 1, i + LOCAL_MIN_RADIUS); j++) {
            if (j != i && similarities.get(j) < v - LOCAL_MIN_EPSILON) {
                return false;
            }
        }
        return true;
    }

    private double percentile(List<Double> sorted, double p) {
        int idx = (int) Math.floor(p * (sorted.size() - 1));
        return sorted.get(Math.min(Math.max(idx, 0), sorted.size() - 1));
    }

    // ==================================================================
    // 主流程步骤 5：组装父块
    // ==================================================================

    private List<List<SemanticUnit>> groupUnitsBySplitPoints(List<SemanticUnit> units, List<Integer> splitPoints) {
        List<List<SemanticUnit>> groups = new ArrayList<>();
        int start = 0;
        for (int sp : splitPoints) {
            groups.add(new ArrayList<>(units.subList(start, sp)));
            start = sp;
        }
        groups.add(new ArrayList<>(units.subList(start, units.size())));
        // 防御：切分点异常时可能产生空组，剔除
        groups.removeIf(List::isEmpty);
        return groups;
    }

    /**
     * 超长组按单元边界二次拆分（例如无切分点时整篇文档成一组的情况），保证父块不超过 maxGroupTokens
     */
    private void splitOversizedGroups(List<List<SemanticUnit>> groups, int maxGroupTokens) {
        int i = 0;
        while (i < groups.size()) {
            List<SemanticUnit> group = groups.get(i);
            if (groupTokens(group) <= maxGroupTokens) {
                i++;
                continue;
            }
            List<List<SemanticUnit>> parts = new ArrayList<>();
            List<SemanticUnit> current = new ArrayList<>();
            int tokens = 0;
            for (SemanticUnit unit : group) {
                int ut = estimateTokens(unit.text());
                if (tokens + ut > maxGroupTokens && !current.isEmpty()) {
                    parts.add(current);
                    current = new ArrayList<>();
                    tokens = 0;
                }
                current.add(unit);
                tokens += ut;
            }
            if (!current.isEmpty()) {
                parts.add(current);
            }
            groups.remove(i);
            groups.addAll(i, parts);
            i += parts.size();
        }
    }

    /**
     * 过小组就近双向合并：优先与合并后 token 更小且不超 chunkSize 的邻居合并
     */
    private void mergeSmallGroups(List<List<SemanticUnit>> groups, int chunkSize) {
        int i = 0;
        while (groups.size() > 1 && i < groups.size()) {
            int curTokens = groupTokens(groups.get(i));
            if (curTokens >= MIN_UNIT_TOKENS) {
                i++;
                continue;
            }
            int prevTokens = i > 0 ? groupTokens(groups.get(i - 1)) : Integer.MAX_VALUE;
            int nextTokens = i < groups.size() - 1 ? groupTokens(groups.get(i + 1)) : Integer.MAX_VALUE;
            boolean canPrev = i > 0 && prevTokens + curTokens <= chunkSize;
            boolean canNext = i < groups.size() - 1 && nextTokens + curTokens <= chunkSize;
            boolean mergePrev;
            boolean mergeNext;
            if (canPrev && canNext) {
                mergePrev = prevTokens <= nextTokens;
                mergeNext = !mergePrev;
            } else {
                mergePrev = canPrev;
                mergeNext = canNext;
            }
            if (mergePrev) {
                groups.get(i - 1).addAll(groups.get(i));
                groups.remove(i);
                i = Math.max(0, i - 1);
            } else if (mergeNext) {
                groups.get(i).addAll(groups.get(i + 1));
                groups.remove(i + 1);
            } else {
                // 两个方向合并都会超限：保留小块，交由子块切分兜底
                i++;
            }
        }
        // 兜底：末尾残留小块向前合并（即便略超 chunkSize，也优于碎片）
        if (groups.size() > 1 && groupTokens(groups.getLast()) < MIN_UNIT_TOKENS) {
            groups.get(groups.size() - 2).addAll(groups.getLast());
            groups.remove(groups.size() - 1);
        }
    }

    /**
     * 物化为父块：标题拼入父块文本，保证 parent_text 上下文完整，
     * 且子块 embedding 能覆盖标题式 query（否则标题只存在于 metadata，不参与向量检索）
     */
    private ParentChunk toParentChunk(List<SemanticUnit> group) {
        String text = group.stream().map(SemanticUnit::text)
            .collect(Collectors.joining("\n\n")).trim();
        String heading = group.getFirst().heading();
        String fullText = StringUtils.isNotEmpty(heading) ? heading + "\n\n" + text : text;
        return new ParentChunk(fullText, heading, group.getFirst().startIndex());
    }

    // ==================================================================
    // 主流程步骤 7：父块间重叠
    // ==================================================================

    /**
     * 相邻父块注入句子级重叠。
     * 重叠始终取自原始文本快照（而非已被注入重叠的前一个父块），避免重叠内容跨父块级联累积。
     * 重叠取尾部句子，标题在头部，实际不会入选重叠内容
     */
    private void addOverlapBetweenParents(List<ParentChunk> parents) {
        if (parents.size() < 2) {
            return;
        }
        List<String> originals = parents.stream().map(ParentChunk::text).collect(Collectors.toList());
        for (int i = 1; i < parents.size(); i++) {
            String overlap = extractTailOverlap(originals.get(i - 1));
            if (StringUtils.isNotEmpty(overlap)) {
                ParentChunk cur = parents.get(i);
                parents.set(i, new ParentChunk(overlap + "\n\n" + cur.text(), cur.heading(), cur.startIndex()));
            }
        }
    }

    /**
     * 从上一父块原文尾部截取约 12% 长度的句子级重叠（限制在 30~200 token 之间）
     */
    private String extractTailOverlap(String prevText) {
        int totalTokens = estimateTokens(prevText);
        int overlapTokens = (int) (totalTokens * PARENT_OVERLAP_RATIO);
        overlapTokens = Math.max(PARENT_OVERLAP_MIN_TOKENS, Math.min(PARENT_OVERLAP_MAX_TOKENS, overlapTokens));
        if (overlapTokens >= totalTokens) {
            return "";
        }
        List<String> sentences = splitToSentencesKeepText(prevText);
        StringBuilder tail = new StringBuilder();
        int count = 0;
        for (int i = sentences.size() - 1; i >= 0 && count < overlapTokens; i--) {
            String s = sentences.get(i).trim();
            tail.insert(0, s + " ");
            count += estimateTokens(s);
        }
        String result = tail.toString().trim();
        return result.length() < prevText.length() ? result : "";
    }

    // ==================================================================
    // 主流程步骤 8：父子结构构建
    // ==================================================================

    /**
     * 解析文档唯一标识：优先取 metadata.doc_id，缺失时生成随机 UUID（保证 parent_id 跨文档全局唯一）
     */
    private String resolveDocId(Document document) {
        Map<String, Object> meta = document.getMetadata();
        Object docId = meta == null ? null : meta.get(DOC_ID_METADATA_KEY);
        return docId != null ? String.valueOf(docId) : UUID.randomUUID().toString();
    }

    /**
     * 构建父子结构：子块用于向量检索，父块通过 metadata 回溯（检索端需读取 parent_text 作为 LLM 上下文）。
     * parent_id 格式为 "{docId}-parent-{p}"，跨文档全局唯一
     */
    private List<Document> buildParentChildDocuments(List<ParentChunk> parents,
                                                     Map<String, Object> metadata,
                                                     int chunkSize,
                                                     String docId) {
        List<Document> children = new ArrayList<>();
        int childSize = Math.max(MIN_CHILD_TOKENS, (int) (chunkSize * CHILD_SIZE_RATIO));
        int childOverlapTokens = (int) (childSize * CHILD_OVERLAP_RATIO);
        // 防御：metadata 为 null 时 NPE
        Map<String, Object> baseMeta = metadata == null ? new HashMap<>() : metadata;

        for (int p = 0; p < parents.size(); p++) {
            ParentChunk parent = parents.get(p);
            if (StringUtils.isEmpty(parent.text())) {
                continue;
            }
            String parentId = docId + "-parent-" + p;
            List<String> childTexts = splitChildWithOverlap(parent.text(), childSize, childOverlapTokens);
            for (int c = 0; c < childTexts.size(); c++) {
                Map<String, Object> meta = new HashMap<>(baseMeta);
                meta.put("parent_id", parentId);
                // 父块全文随子块存储：检索命中子块后直接回溯，无需二次查询。
                // 大规模场景建议改为父块单独建表 + parent_id 引用，避免向量库存储膨胀
                meta.put("parent_text", parent.text());
                meta.put("chunk_index", p);
                meta.put("child_index", c);
                meta.put("start_index", parent.startIndex());
                if (StringUtils.isNotEmpty(parent.heading())) {
                    meta.put("heading", parent.heading());
                }
                Document childDoc = Document.builder().text(childTexts.get(c)).metadata(meta).build();
                // 单句超长等极端情况兜底：走 Token 切分器（metadata 会被继承）
                if (estimateTokens(childTexts.get(c)) > childSize * 2) {
                    children.addAll(buildTokenSplitter(chunkSize).apply(List.of(childDoc)));
                } else {
                    children.add(childDoc);
                }
            }
        }
        return children;
    }

    /**
     * 父块内按句子累积切子块，子块间携带约 15% 的尾部句子重叠
     */
    private List<String> splitChildWithOverlap(String text, int childSize, int overlapTokens) {
        if (estimateTokens(text) <= childSize) {
            return List.of(text);
        }
        List<String> sentences = splitToSentencesKeepText(text);
        List<String> children = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        int bufferTokens = 0;
        for (String sentence : sentences) {
            int st = estimateTokens(sentence);
            if (bufferTokens + st > childSize && !buffer.isEmpty()) {
                children.add(String.join("", buffer).trim());
                // 携带尾部句子作为下一子块的重叠
                List<String> overlapBuffer = new ArrayList<>();
                int overlapCount = 0;
                for (int i = buffer.size() - 1; i >= 0 && overlapCount < overlapTokens; i--) {
                    overlapBuffer.add(0, buffer.get(i));
                    overlapCount += estimateTokens(buffer.get(i));
                }
                buffer = new ArrayList<>(overlapBuffer);
                bufferTokens = overlapCount;
            }
            buffer.add(sentence);
            bufferTokens += st;
        }
        if (!buffer.isEmpty()) {
            String last = String.join("", buffer).trim();
            if (children.isEmpty() || !last.equals(children.getLast())) {
                children.add(last);
            }
        }
        return children;
    }

    // ==================================================================
    // 降级与工具方法
    // ==================================================================

    /**
     * Token 分块降级方案
     */
    private List<Document> tokenSplit(Document document, int chunkSize) {
        return buildTokenSplitter(chunkSize).apply(List.of(document));
    }

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
     * 文本清洗：统一换行符（CRLF/CR → LF，Windows 文档兼容）、连续空白归一，
     * 可选删除 URL/邮箱（对齐 Dify 预处理规则）。
     * 换行符统一必须在其他归一之前："\n\n" 分隔符切分与空白归一都依赖 LF
     */
    private String cleanText(String text) {
        String cleaned = text.replace("\r\n", "\n").replace('\r', '\n');
        if (REMOVE_URL_AND_EMAIL) {
            cleaned = URL_PATTERN.matcher(cleaned).replaceAll(" ");
            cleaned = EMAIL_PATTERN.matcher(cleaned).replaceAll(" ");
        }
        cleaned = cleaned.replaceAll("[ \\t\\x0B\\f]+", " ");
        cleaned = cleaned.replaceAll(" ?\\n ?", "\n");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        return cleaned.trim();
    }

    /**
     * 估算文本的 Token 数（短文本带 LRU 缓存，避免分块过程中重复 encode；超长文本直接计算不入缓存）
     */
    private int estimateTokens(String text) {
        if (StringUtils.isEmpty(text)) {
            return 0;
        }
        if (text.length() > TOKEN_CACHE_MAX_TEXT_LENGTH) {
            return TOKEN_ENCODING.encode(text).size();
        }
        Integer cached = TOKEN_CACHE.get(text);
        if (cached != null) {
            return cached;
        }
        int count = TOKEN_ENCODING.encode(text).size();
        TOKEN_CACHE.put(text, count);
        return count;
    }

    private int groupTokens(List<SemanticUnit> units) {
        if (units.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (SemanticUnit unit : units) {
            total += estimateTokens(unit.text()) + 1; // +1 为单元间连接符开销
        }
        return total - 1;
    }

    /**
     * 向量组逐维平均向量
     */
    private float[] averageEmbedding(List<float[]> vectors) {
        if (vectors.isEmpty()) {
            return new float[0];
        }
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
     * 余弦相似度
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
}
