package com.mall.aichat.chunker;

import com.mall.aichat.domain.KbDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档切分管线：结构化分块优先，智能切分兜底。
 *
 * <pre>
 * 清洗后的 markdown
 *   ├─ MarkdownStructurer 按结构分块（标题/水平线/代码块）
 *   │    ├─ 块为 markdown 表格（Excel 大表）→ 表头回填切分
 *   │    ├─ 块超长 → ChunkerFactory 二次切
 *   │    └─ 其余 → 直接作为 chunk
 *   └─ 无结构（纯文本/扫描件）→ ChunkerFactory 兜底
 * </pre>
 *
 * @author mall
 */
@Component
public class DocumentChunkPipeline {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkPipeline.class);

    /** 超过此长度倍数的结构块触发二次切分（chunkSize 的 2 倍，留出余量） */
    private static final int OVERSIZED_MULTIPLIER = 2;

    /** kbDocument 未指定 chunkSize 时的默认值 */
    private static final int DEFAULT_CHUNK_SIZE = 500;

    /** 结构有效性判定前缀：category 取值 header_1~header_6，必须 startsWith 而非 equals */
    private static final String HEADER_CATEGORY_PREFIX = "header";

    private final MarkdownStructurer structurer;

    private final ChunkerFactory chunkerFactory;

    public DocumentChunkPipeline(MarkdownStructurer structurer,
                                 @Autowired(required = false) ChunkerFactory chunkerFactory) {
        this.structurer = structurer;
        this.chunkerFactory = chunkerFactory;
    }

    /**
     * 将清洗后的 markdown 全文切分为入库块。
     *
     * @param cleanedContent 清洗后的 markdown 全文
     * @param kbDocument     文档实体，提供业务元数据与切分参数
     * @return 切分后的 {@link Document} 列表
     */
    public List<Document> toChunks(String cleanedContent, KbDocument kbDocument) {
        Map<String, Object> bizMeta = buildBizMeta(kbDocument);
        int chunkSize = kbDocument.getChunkSize() != null
                ? kbDocument.getChunkSize().intValue() : DEFAULT_CHUNK_SIZE;

        List<Document> structured = structurer.structure(cleanedContent, bizMeta);

        // 结构有效性：存在 header 类块才算切出了结构
        boolean hasStructure = structured.stream().anyMatch(d ->
                String.valueOf(d.getMetadata().getOrDefault("category", ""))
                        .startsWith(HEADER_CATEGORY_PREFIX));

        if (!hasStructure) {
            // 纯文本 / 无标题文档 / 扫描件近空内容 → 原有智能切分兜底
            log.debug("未识别到标题结构，走智能切分兜底，filename={}", bizMeta.get("filename"));
            return fallbackChunk(new Document(cleanedContent, bizMeta), kbDocument, chunkSize);
        }

        List<Document> chunks = new ArrayList<>();
        int tableBlocks = 0;
        int oversizedBlocks = 0;
        int plainBlocks = 0;
        for (Document block : structured) {
            String text = block.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            if (isMarkdownTable(text)) {
                // Excel 大表：Token/语义切分会把表格拦腰切断，必须表头回填按行分组
                chunks.addAll(splitMarkdownTable(block, chunkSize * OVERSIZED_MULTIPLIER));
                tableBlocks++;
            } else if (text.length() > chunkSize * OVERSIZED_MULTIPLIER) {
                // 无子标题的长章节（如一整章连续段落）→ 二次智能切分
                chunks.addAll(oversizedChunk(block, kbDocument, chunkSize));
                oversizedBlocks++;
            } else {
                chunks.add(block);
                plainBlocks++;
            }
        }
        log.debug("结构化分块完成：table={}, oversized={}, plain={}, totalChunks={}, filename={}",
                tableBlocks, oversizedBlocks, plainBlocks, chunks.size(), bizMeta.get("filename"));
        return chunks;
    }

    /**
     * 构建业务元数据。
     * <p>
     * 向量库对 metadata 值类型敏感（多数只接受 String/Number），统一 {@code String.valueOf} 转换；
     * 同时过滤 null，避免 {@link Document} 的非空断言导致整篇入库失败。
     */
    private Map<String, Object> buildBizMeta(KbDocument kbDocument) {
        Map<String, Object> meta = new HashMap<>();
        putIfNotNull(meta, "filename", kbDocument.getFileName());
        putIfNotNull(meta, "knowledgeId", kbDocument.getKnowledgeId());
        putIfNotNull(meta, "source", kbDocument.getFilePath());
        return meta;
    }

    private void putIfNotNull(Map<String, Object> meta, String key, Object value) {
        if (value != null) {
            meta.put(key, String.valueOf(value));
        }
    }

    // ==================================================================
    // 表格感知切分：按行分组，每组重放表头行 + 分隔行
    // ==================================================================

    private boolean isMarkdownTable(String text) {
        return text.startsWith("|") && text.contains("\n|");
    }

    private List<Document> splitMarkdownTable(Document block, int maxChars) {
        List<String> lines = List.of(block.getText().split("\n"));
        // 定位表头行（首个 | 开头行）+ 分隔行（含 ---）
        int headerIdx = -1;
        for (int i = 0; i < lines.size() - 1; i++) {
            if (lines.get(i).startsWith("|")
                    && lines.get(i + 1).startsWith("|")
                    && lines.get(i + 1).contains("---")) {
                headerIdx = i;
                break;
            }
        }
        if (headerIdx < 0) {
            return List.of(block);   // 不是规范表格，交回普通处理
        }
        String header = lines.get(headerIdx) + "\n" + lines.get(headerIdx + 1);
        List<Document> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder(header);
        int part = 1;
        for (int i = headerIdx + 2; i < lines.size(); i++) {
            String line = lines.get(i);
            if (cur.length() + line.length() + 1 > maxChars && cur.length() > header.length()) {
                result.add(copyWithMeta(block, cur.toString(), part++));
                cur = new StringBuilder(header);   // 每组重放表头，检索语义完整
            }
            cur.append('\n').append(line);
        }
        if (cur.length() > header.length()) {
            result.add(copyWithMeta(block, cur.toString(), part));
        }
        log.debug("table block split into {} chunks, filename={}", result.size(),
                block.getMetadata().get("filename"));
        return result;
    }

    /** 复制块的 metadata 并追加表格分片序号，便于检索后还原顺序 */
    private Document copyWithMeta(Document source, String text, int part) {
        Map<String, Object> meta = new HashMap<>(source.getMetadata());
        meta.put("tablePart", part);
        return new Document(text, meta);
    }

    // ==================================================================
    // 二次切分 / 兜底（ChunkerFactory 可能未装配，保持现有容错语义）
    // ==================================================================

    private List<Document> oversizedChunk(Document block, KbDocument kbDocument, int chunkSize) {
        List<Document> split = doSmartChunk(block, kbDocument, chunkSize);
        // 继承结构块的 title 元数据，二次切出的碎片也能按章节过滤
        Object title = block.getMetadata().get("title");
        if (title != null) {
            split.forEach(d -> d.getMetadata().put("title", title));
        }
        return split;
    }

    private List<Document> fallbackChunk(Document document, KbDocument kbDocument, int chunkSize) {
        List<Document> split = doSmartChunk(document, kbDocument, chunkSize);
        return split.isEmpty() ? List.of(document) : split;
    }

    private List<Document> doSmartChunk(Document document, KbDocument kbDocument, int chunkSize) {
        if (chunkerFactory == null) {
            log.warn("ChunkerFactory 未配置，整块返回不做切分。");
            return List.of(document);
        }
        return chunkerFactory.chunk(document,
                Boolean.TRUE.equals(kbDocument.getSemanticChunking()),
                chunkSize, kbDocument.getChunkSeparator());
    }
}
