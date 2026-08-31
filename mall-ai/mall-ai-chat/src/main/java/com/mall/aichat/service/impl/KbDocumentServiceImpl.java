package com.mall.aichat.service.impl;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.mall.aichat.domain.KbDocument;
import com.mall.aichat.domain.KbDocumentChunk;
import com.mall.aichat.domain.MinerUResult;
import com.mall.aichat.mapper.KbDocumentMapper;
import com.mall.aichat.service.IKbDocumentChunkService;
import com.mall.aichat.service.IKbDocumentService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.domain.R;
import com.mall.common.core.utils.DateUtils;
import com.mall.common.core.utils.StringUtils;
import com.mall.common.core.utils.uuid.IdUtils;
import com.mall.system.api.RemoteFileService;
import com.mall.system.api.domain.SysFile;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 知识库文档Service业务层处理
 *
 * @author mall
 * @date 2026-07-05
 */
@Service
public class KbDocumentServiceImpl implements IKbDocumentService {
    private static final Logger log = LoggerFactory.getLogger(KbDocumentServiceImpl.class);

    // ============ 语义分块参数 ============
    /** 滑动窗口大小：断点两侧各取 WINDOW_SIZE 个单元拼接成缓冲句再比相似度，抗单点噪声（参考 LlamaIndex SemanticSplitter） */
    private static final int WINDOW_SIZE = 1;
    /** 动态切分阈值百分位：相似度最低的 SPLIT_PERCENTILE 分位作为切分点，避免固定阈值对不同 embedding 模型失效 */
    private static final double SPLIT_PERCENTILE = 0.20;
    /** 语义分块阈值计算的最小间隔样本数，样本过少时不硬编码阈值，降级为 Token 分块 */
    private static final int MIN_GAP_SAMPLES = 4;
    /** 最小语义单元/语义块字符数：短句合并上限，也是块合并下限，避免过小单元导致向量噪声大 */
    private static final int MIN_UNIT_CHARS = 250;
    /** Token 编码器：与 TokenTextSplitter 的 CL100K_BASE 保持一致，用于估算文本 Token 数 */
    private static final EncodingRegistry ENCODING_REGISTRY = Encodings.newLazyEncodingRegistry();

    @Autowired
    private KbDocumentMapper kbDocumentMapper;

    @Autowired(required = false)
    @Qualifier("knowledgeVectorStore")
    private VectorStore knowledgeVectorStore;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Autowired
    private IKbDocumentChunkService iKbDocumentChunkService;

    @Autowired
    private RemoteFileService remoteFileService;

    @Resource(name = "minerUChatClient")
    private ChatClient minerUChatClient;

    @Autowired
    private MinerUService minerUService;

    /**
     * 查询知识库文档
     *
     * @param id 知识库文档主键
     * @return 知识库文档
     */
    @Override
    public KbDocument selectKbDocumentById(String id) {
        return kbDocumentMapper.selectKbDocumentById(id);
    }

    /**
     * 查询知识库文档列表
     *
     * @param kbDocument 知识库文档
     * @return 知识库文档
     */
    @Override
    public List<KbDocument> selectKbDocumentList(KbDocument kbDocument) {
        return kbDocumentMapper.selectKbDocumentList(kbDocument);
    }

    /**
     * 新增知识库文档
     *
     * @param kbDocument 知识库文档
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertKbDocument(KbDocument kbDocument) {
        int i = 0;
        try {
            kbDocument.setCreateTime(DateUtils.getNowDate());
            kbDocument.setId(IdUtils.fastUUID());
            i = kbDocumentMapper.insertKbDocument(kbDocument);

            // 获取并解析文件
            R<SysFile> fileR = remoteFileService.getFile(kbDocument.getFilePath());
            if (fileR.getCode() != Constants.SUCCESS || fileR.getData() == null) {
                throw new RuntimeException("远程获取文件失败: " + (fileR.getMsg() != null ? fileR.getMsg() : "未知错误"));
            }

            String filePath = fileR.getData().getUrl();
            FileSystemResource fileResource = new FileSystemResource(filePath);
            String filename = fileResource.getFilename();

            // 提取纯文本/Markdown内容
            String rawContent = this.extractText(fileResource, filename);
            if (StringUtils.isEmpty(rawContent)) {
                throw new RuntimeException("文档内容提取失败，内容为空");
            }

            // 内容清洗 (移除冗余标签)
            String cleanedContent = this.cleanContent(rawContent);

            Document document = Document.builder()
                .text(cleanedContent)
                .metadata(Map.of(
                    "filename", kbDocument.getFileName(),
                    "knowledgeId", kbDocument.getKnowledgeId(),
                    "source", kbDocument.getFilePath()
                ))
                .build();

            // 通用智能切分 (根据是否开启语义分块选择策略)
            List<Document> chunks = this.generalSmartSplit(document, kbDocument);

            // 存入向量库
            if (knowledgeVectorStore != null) {
                knowledgeVectorStore.add(chunks);
            } else {
                log.warn("VectorStore 未配置，跳过向量化步骤。");
            }

            // 8. 同步 MySQL Chunk 记录
            List<KbDocumentChunk> dbChunks = chunks.stream().map(chunk -> {
                KbDocumentChunk dbChunk = new KbDocumentChunk();
                dbChunk.setId(chunk.getId());
                dbChunk.setDocumentId(kbDocument.getId());
                dbChunk.setKnowledgeId(kbDocument.getKnowledgeId());
                dbChunk.setContent(chunk.getText());
                return dbChunk;
            }).toList();

            dbChunks.forEach(iKbDocumentChunkService::insertKbDocumentChunk);
        } catch (Exception e) {
            kbDocument.setStatus(1L); // 失败
            kbDocumentMapper.updateKbDocument(kbDocument);
            throw new RuntimeException("文档处理失败", e);
        }

        return i;
    }

    @Override
    public List<KbDocument> selectDocumentsByTags(String tags, String kbType, String status) {
        return kbDocumentMapper.selectDocumentsByTags(tags, kbType, status);
    }

    @Override
    public List<KbDocument> selectDocumentsByKbType(String kbType, String status) {
        return kbDocumentMapper.selectDocumentsByKbType(kbType, status);
    }

    /**
     * 提取文本内容
     * 参照Dify，按文件类型路由到不同的提取器
     */
    private String extractText(FileSystemResource fileResource, String filename) throws Exception {
        String lowerFilename = filename.toLowerCase();

        if (lowerFilename.endsWith(".md")) {
            return Files.readString(fileResource.getFile().toPath());
        } else if (isImageFile(lowerFilename)) {
            return parseImageWithLLM(fileResource);
        } else {
            // 默认使用 MinerU 处理 PDF/Word 等复杂文档
            MinerUResult minerUResult = minerUService.parseMarkdown(fileResource);
            String markdownContent = minerUResult.markdown();

            // 处理文档内嵌的图片：将图片转为文本描述替换回原文
            if (minerUResult.images() != null && !minerUResult.images().isEmpty()) {
                for (MinerUResult.ImageData imageData : minerUResult.images()) {
                    String imageText = parseImageWithLLM(new ByteArrayResource(imageData.data()));
                    String imagePlaceholder = "![](images/" + imageData.name() + ")";
                    // 防止替换失败，使用正则
                    String replacementText = "\n[图片说明: " + imageText + "]\n";
                    markdownContent = markdownContent.replace(imagePlaceholder, replacementText);
                }
            }
            return markdownContent;
        }
    }

    /**
     * 规则清洗：处理明显的 OCR 噪声
     */
    private String cleanContent(String rawText) {
        if (rawText == null) return "";

        // 1. 移除 MinerU 等工具的特殊标签
        String cleanedText = rawText.replaceAll("(<\\|txt_contd_tgt\\|>|<\\|txt_contd_src\\|>)+", "");

        // 2. 移除仅含数字的独立行 (通常是页码)
        cleanedText = cleanedText.replaceAll("(?m)^\\s*\\d+\\s*$", "");

        // 3. 【新增】移除常见的 OCR 噪声字符，如连续的乱码符号
        cleanedText = cleanedText.replaceAll("[■□▲△○●⊛※]{2,}", "");

        // 4. 【新增】处理 OCR 常见的多余空格，特别是中文字符之间的空格
        cleanedText = cleanedText.replaceAll("([\\u4e00-\\u9fa5])\\s+([\\u4e00-\\u9fa5])", "$1$2");

        // 5. 规范化空行
        cleanedText = cleanedText.replaceAll("\\n{3,}", "\n\n");

        return cleanedText.trim();
    }

    /**
     * 通用智能切分策略
     * 根据是否开启语义分块，选择不同的分块策略：
     * - 开启语义分块：使用 EmbeddingModel 计算段落向量，按语义相似度合并段落
     * - 未开启：先按段落粗切分，再按 Token 细切分
     */
    private List<Document> generalSmartSplit(Document document, KbDocument kbDocument) {
        // 判断是否开启语义分块
        boolean semanticEnabled = Boolean.TRUE.equals(kbDocument.getSemanticChunking());

        if (semanticEnabled) {
            return semanticSplit(document, kbDocument);
        }

        // 原有逻辑：固定 token / 段落分块
        // 1. 先按段落进行粗切分，保证语义完整性
        String text = document.getText();
        String separator = StringUtils.isNotEmpty(kbDocument.getChunkSeparator()) ? kbDocument.getChunkSeparator() : "\\n\\n";
        String[] paragraphs = text.split(separator); // 按自定义分段符分段，默认按空行
        List<Document> preSplitDocs = new ArrayList<>();

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (StringUtils.isNotEmpty(trimmed)) {
                preSplitDocs.add(new Document(trimmed, document.getMetadata()));
            }
        }

        // 如果粗切分后只有一段，说明是没有标准段落的文档，直接交给 Token 切分器
        if (preSplitDocs.size() <= 1) {
            preSplitDocs = List.of(document);
        }

        // 2. Token 级别细切分，防止超长
        int chunkSize = kbDocument.getChunkSize() != null ? kbDocument.getChunkSize().intValue() : 500;

        // 参考 Dify: 提供重叠度以保留上下文，虽然 Spring AI 的 TokenTextSplitter 不直接支持 overlap，
        // 但可以通过设置 minChunkSizeChars 来尽量保证完整性
        return buildTokenSplitter(chunkSize).apply(preSplitDocs);
    }

    /**
     * 语义分块策略（滑动窗口 + 动态阈值，参考 LlamaIndex SemanticSplitter）
     * 算法步骤：
     * 1. 将文档按段落/句子切分为最小单元（短句合并到 MIN_UNIT_CHARS，保证单元向量质量）
     * 2. 对每个单元计算 embedding 向量
     * 3. 滑动窗口计算断点相似度：断点两侧各取 WINDOW_SIZE 个单元拼接后再比，抗单点噪声
     * 4. 动态阈值：取相似度最低的 SPLIT_PERCENTILE 分位作为切分点，不依赖模型特定的绝对相似度分布
     * 5. 按切分点合并单元形成语义块，小于 MIN_UNIT_CHARS 的块并入相邻块（先写后并，不丢内容）
     * 6. 按 Token 数（而非字符数）判断超长，超过 chunkSize 的块用 TokenTextSplitter 细切分
     */
    private List<Document> semanticSplit(Document document, KbDocument kbDocument) {
        String text = document.getText();
        int chunkSize = kbDocument.getChunkSize() != null ? kbDocument.getChunkSize().intValue() : 500;

        // 1. 按段落/句子切分为最小单元（短句已合并到 MIN_UNIT_CHARS）
        List<String> units = splitIntoSemanticUnits(text, kbDocument.getChunkSeparator(), chunkSize);

        if (units.size() <= 1) {
            // 只有一个单元，仍需保证不超 Token 上限，交给 Token 切分器兜底（短文本会原样返回）
            return buildTokenSplitter(chunkSize).apply(List.of(document));
        }

        // 单元数不足以构成一个滑动窗口断点时，语义比较无意义，直接走 Token 分块（同样控制长度）
        if (units.size() < 2 * WINDOW_SIZE + 1) {
            log.info("语义单元数 {} 过少，无法构成滑动窗口断点，采用 Token 分块", units.size());
            return buildTokenSplitter(chunkSize).apply(List.of(document));
        }

        // 2. 如果未配置 EmbeddingModel，降级为 Token 分块
        if (embeddingModel == null) {
            log.warn("EmbeddingModel 未配置，语义分块降级为 Token 分块");
            return tokenSplitFallback(document, kbDocument, chunkSize);
        }

        // 3. 计算每个单元的 embedding 向量
        List<float[]> embeddings = new ArrayList<>();
        try {
            // 分批调用 embedding，避免一次请求过大
            int batchSize = 20;
            for (int i = 0; i < units.size(); i += batchSize) {
                int end = Math.min(i + batchSize, units.size());
                List<String> batch = units.subList(i, end);
                List<float[]> batchEmbeddings = embeddingModel.embed(batch);
                embeddings.addAll(batchEmbeddings);
            }
        } catch (Exception e) {
            log.error("Embedding 计算失败，语义分块降级为 Token 分块", e);
            return tokenSplitFallback(document, kbDocument, chunkSize);
        }

        // 4. 滑动窗口计算断点相似度：断点 p 处比较 [p-WINDOW_SIZE, p) 与 [p, p+WINDOW_SIZE) 两段缓冲句，
        //    相比逐对比较，单个噪声向量被窗口内其它向量稀释，抗噪能力更强（LlamaIndex buffer_size=1）
        List<Integer> gaps = new ArrayList<>();
        List<Double> gapSimilarities = new ArrayList<>();
        for (int p = WINDOW_SIZE; p <= units.size() - WINDOW_SIZE; p++) {
            float[] left = averageEmbedding(embeddings.subList(p - WINDOW_SIZE, p));
            float[] right = averageEmbedding(embeddings.subList(p, p + WINDOW_SIZE));
            gaps.add(p);
            gapSimilarities.add(cosineSimilarity(left, right));
        }

        // 5. 动态阈值：取相似度最低的 SPLIT_PERCENTILE 分位作为切分阈值。
        //    不同 embedding 模型的绝对相似度分布差异极大（bge 中文普遍 0.8+，text-embedding-3 可能 0.3 左右），
        //    固定阈值对部分模型会退化为"永不切分"或"处处切分"，百分位阈值自适应文档自身的相似度分布。
        if (gapSimilarities.size() < MIN_GAP_SAMPLES) {
            // 样本过少时不硬编码阈值（会导致全篇合一巨块），降级为 Token 分块控制块长
            log.info("语义断点样本数 {} 不足，跳过分位数阈值，降级为 Token 分块", gapSimilarities.size());
            return tokenSplitFallback(document, kbDocument, chunkSize);
        }
        List<Double> sorted = gapSimilarities.stream().sorted().collect(Collectors.toList());
        int idx = Math.max(0, (int) Math.floor(SPLIT_PERCENTILE * (sorted.size() - 1)));
        double splitThreshold = sorted.get(idx);

        List<Integer> splitPoints = new ArrayList<>();
        for (int g = 0; g < gaps.size(); g++) {
            // 该处相似度显著低于文档整体分布，判定为语义边界（切分点为单元下标，即在第 p-1 与 p 单元之间）
            if (gapSimilarities.get(g) < splitThreshold) {
                splitPoints.add(gaps.get(g));
            }
        }

        // 6. 按切分点合并相邻单元，形成语义块（先写后并，不丢内容）
        List<String> chunkTexts = new ArrayList<>();
        int start = 0;
        for (int splitPoint : splitPoints) {
            chunkTexts.add(String.join("\n\n", units.subList(start, splitPoint)).trim());
            start = splitPoint;
        }
        chunkTexts.add(String.join("\n\n", units.subList(start, units.size())).trim());

        // 7. 合并过小的语义块：小于 MIN_UNIT_CHARS 的块检索时上下文不足，并入相邻块（后一块临近 Token 上限时向前合并）
        chunkTexts = mergeSmallChunks(chunkTexts, chunkSize);

        List<Document> semanticChunks = new ArrayList<>();
        for (String chunkText : chunkTexts) {
            if (StringUtils.isNotEmpty(chunkText)) {
                semanticChunks.add(Document.builder().text(chunkText).metadata(document.getMetadata()).build());
            }
        }

        // 8. 按 Token 数（与 TokenTextSplitter 的 CL100K_BASE 口径一致）判断超长并细切分；
        //    中文 1 字符 ≈ 0.6~1 token，直接用字符数与 chunkSize 比较会导致超长块漏切或正常块误切，故统一用 Token 数判断。
        List<Document> finalChunks = new ArrayList<>();
        for (Document chunk : semanticChunks) {
            if (estimateTokens(chunk.getText()) > chunkSize) {
                finalChunks.addAll(buildTokenSplitter(chunkSize).apply(List.of(chunk)));
            } else {
                finalChunks.add(chunk);
            }
        }

        log.info("语义分块完成: 最小单元 {} 个，语义断点 {} 个（阈值={}），最终块 {} 个",
            units.size(), splitPoints.size(), String.format("%.4f", splitThreshold), finalChunks.size());
        return finalChunks;
    }

    /**
     * 计算向量组逐维平均向量（用于滑动窗口缓冲句的合成向量）
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
     * 合并过小的语义块：小于 MIN_UNIT_CHARS 的块并入下一块；末尾小块并入上一块。
     * 若相邻块已接近 chunkSize Token 上限，则跳过强制合并，交由后续 Token 细切分处理，避免产生超大块。
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
        // 末尾残留的小块向前合并，避免最后一个块过小；若合并后超限则保留原状（由 Token 细切分兜底）
        if (merged.size() > 1 && merged.getLast().length() < MIN_UNIT_CHARS
            && estimateTokens(merged.get(merged.size() - 2) + merged.getLast()) <= chunkSize) {
            String last = merged.remove(merged.size() - 1);
            merged.set(merged.size() - 1, merged.getLast() + "\n\n" + last);
        }
        return merged;
    }

    /**
     * 估算文本的 Token 数（CL100K_BASE，与 TokenTextSplitter 口径一致）
     */
    private int estimateTokens(String text) {
        if (StringUtils.isEmpty(text)) {
            return 0;
        }
        return ENCODING_REGISTRY.getEncoding(EncodingType.CL100K_BASE).encode(text).size();
    }

    /**
     * 构建统一配置的 Token 切分器（固定分块、语义分块细切分、降级分块共用）
     */
    private TokenTextSplitter buildTokenSplitter(int chunkSize) {
        return TokenTextSplitter.builder()
            .withChunkSize(chunkSize)
            .withMinChunkSizeChars((int) (chunkSize * 0.1))
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            // 增加更多的标点符号切分支持（中英文）
            .withPunctuationMarks(List.of('.', '?', '!', '。', '？', '！', '\n', ';', '；'))
            .withEncodingType(EncodingType.CL100K_BASE)
            .build();
    }

    /**
     * 将文本切分为语义最小单元（段落/句子）
     * 优先按段落（空行）切分，如果段落超过单块容量则按句子切分，保证单个单元不会占满整个块容量导致窗口比较退化。
     */
    private List<String> splitIntoSemanticUnits(String text, String customSeparator, int chunkSize) {
        List<String> units = new ArrayList<>();
    
        // 1. 按段落切分
        String separator = StringUtils.isNotEmpty(customSeparator) ? customSeparator : "\\n\\n";
        String[] paragraphs = text.split(separator);
    
        // 段落长度上限：大致对应一个块能容纳的最大字符数（中文 1 字符 ≈ 0.6~1 token，取宽裕估计）
        int maxUnitChars = chunkSize * 2;
    
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (StringUtils.isEmpty(trimmed)) {
                continue;
            }
    
            // 2. 如果段落过长（超过单块容量），按句子进一步切分，避免单个单元占满整块导致切分点失效
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
        // 匹配中英文句号、问号、感叹号、分号等
        Pattern pattern = Pattern.compile("[^。！？!?；;]+[。！？!?；;]?");
        Matcher matcher = pattern.matcher(text);

        StringBuilder current = new StringBuilder();
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (StringUtils.isEmpty(sentence)) {
                continue;
            }
            // 合并短句至 MIN_UNIT_CHARS，避免产生过小的语义单元（短句向量噪声大，相邻相似度抖动严重）
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
     * Token 分块降级方案（当 EmbeddingModel 不可用时使用）
     */
    private List<Document> tokenSplitFallback(Document document, KbDocument kbDocument, int chunkSize) {
        return buildTokenSplitter(chunkSize).apply(List.of(document));
    }

    /**
     * 调用多模态LLM解析图片
     */
    private String parseImageWithLLM(org.springframework.core.io.Resource resource) {
        try {
            MimeType mimeType = guessMimeType(resource.getFilename());
            Media media = Media.builder().mimeType(mimeType).data(resource).build();

            return minerUChatClient.prompt()
                .user(u -> u.text("1").media(media))
                .call()
                .content();
        } catch (Exception e) {
            log.error("图片解析失败: {}", e.getMessage());
            return "[图片解析失败]";
        }
    }

    private boolean isImageFile(String filename) {
        return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".gif");
    }

    private MimeType guessMimeType(String filename) {
        if (filename == null) return MimeTypeUtils.IMAGE_PNG;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return MimeTypeUtils.parseMimeType("application/pdf");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MimeTypeUtils.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MimeTypeUtils.IMAGE_GIF;
        return MimeTypeUtils.IMAGE_PNG;
    }

    /**
     * 修改知识库文档
     *
     * @param kbDocument 知识库文档
     * @return 结果
     */
    @Override
    public int updateKbDocument(KbDocument kbDocument) {
        return kbDocumentMapper.updateKbDocument(kbDocument);
    }

    /**
     * 批量删除知识库文档
     *
     * @param ids 需要删除的知识库文档主键
     * @return 结果
     */
    @Override
    public int deleteKbDocumentByIds(String[] ids) {
        for (String id : ids) {
            KbDocument kbDocument = this.selectKbDocumentById(id);
            remoteFileService.delete(kbDocument.getFilePath());

            KbDocumentChunk kbDocumentChunk = new KbDocumentChunk();
            kbDocumentChunk.setDocumentId(id);
            List<KbDocumentChunk> kbDocumentChunks = iKbDocumentChunkService.selectKbDocumentChunkList(kbDocumentChunk);
            if (!kbDocumentChunks.isEmpty()) {
                iKbDocumentChunkService.deleteKbDocumentChunkByIds(kbDocumentChunks.stream().map(KbDocumentChunk::getId).toArray(String[]::new));
            }
        }
        return kbDocumentMapper.deleteKbDocumentByIds(ids);
    }

    /**
     * 删除知识库文档信息
     *
     * @param id 知识库文档主键
     * @return 结果
     */
    @Override
    public int deleteKbDocumentById(String id) {
        return kbDocumentMapper.deleteKbDocumentById(id);
    }

    @Override
    public int deleteKbDocumentByKnowledgeIds(String[] knowledgeIds) {
        KbDocument kbDocument = new KbDocument();
        for (String knowledgeId : knowledgeIds) {
            kbDocument.setKnowledgeId(knowledgeId);
            List<KbDocument> kbDocuments = this.selectKbDocumentList(kbDocument);
            for (KbDocument document : kbDocuments) {
                remoteFileService.delete(document.getFilePath());
            }
        }

        int i = kbDocumentMapper.deleteKbDocumentByKnowledgeIds(knowledgeIds);

        iKbDocumentChunkService.deleteKbDocumentChunkByKnowledgeIds(knowledgeIds);
        return i;
    }
}