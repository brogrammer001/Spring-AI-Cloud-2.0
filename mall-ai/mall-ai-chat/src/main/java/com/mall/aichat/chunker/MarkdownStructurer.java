package com.mall.aichat.chunker;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Markdown 结构化分块器：把 markdown 字符串解析为按标题/水平线/代码块分块的 {@link Document} 列表。
 * <p>
 * 输入不区分来源——.md 文件内容、{@code WordExtractor} / {@code PdfExtractor} / {@code ExcelExtractor}
 * 的输出统一进入此处。本层<b>只做 Reader 的事，不含任何分流逻辑</b>，保证可独立单测。
 *
 * @author mall
 */
@Component
public class MarkdownStructurer {

    /** 分块策略标记，写入每个产出块的 metadata，便于溯源本块由 markdown 结构化产生 */
    private static final String CHUNK_STRATEGY_KEY = "chunkStrategy";

    /** 分块策略标记值 */
    private static final String CHUNK_STRATEGY_MARKDOWN = "markdown";

    /**
     * 将 markdown 全文解析为结构化块。
     *
     * @param markdown 清洗后的 markdown 全文
     * @param bizMeta  业务元数据，注入到每个产出块（随 {@link Document} 一起入向量库）；可为空
     * @return 结构化块列表；输入为空时返回空列表
     */
    public List<Document> structure(String markdown, Map<String, Object> bizMeta) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }

        // Reader 接受任意 Resource：内存字符串包 ByteArrayResource 即可，
        // 内部按流读取，与 cleanContent 之前的处理编码一致（统一 UTF-8）
        Resource resource = new ByteArrayResource(markdown.getBytes(StandardCharsets.UTF_8));

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                // --- 水平线切块（配合 Word [分页] → --- 转换，实现按物理页分块）
                .withHorizontalRuleCreateDocument(true)
                // 代码块随相邻正文合并为一块，metadata 追加 category=code_block 与 lang
                .withIncludeCodeBlock(true)
                // 引用块独立成块（category=blockquote），不污染正文章节
                .withIncludeBlockquote(false)
                .withAdditionalMetadata(mergeMetadata(bizMeta))
                .build();

        return new MarkdownDocumentReader(resource, config).get();
    }

    /**
     * 合并分块策略标记与业务元数据。
     * <p>
     * {@link Document} 与 {@link MarkdownDocumentReaderConfig.Builder} 均断言 metadata 值非空，
     * 因此此处过滤 null 键/值，避免个别字段缺失时整篇入库失败。
     */
    private Map<String, Object> mergeMetadata(Map<String, Object> bizMeta) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(CHUNK_STRATEGY_KEY, CHUNK_STRATEGY_MARKDOWN);
        if (bizMeta != null) {
            bizMeta.forEach((k, v) -> {
                if (k != null && v != null) {
                    metadata.put(k, v);
                }
            });
        }
        return metadata;
    }
}
