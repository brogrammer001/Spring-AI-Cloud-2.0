package com.mall.aichat.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * PDF 文档提取器
 * <p>
 * 走基类 Tika 结构化路径 {@link AbstractTikaExtractor#parseTikaToMarkdown}：
 * {@code ToXMLContentHandler} 收 XHTML SAX 事件流式转 Markdown，
 * 保留 PDF 的标题层级 / 表格 / 列表结构（优于纯文本拍平）。
 * Tika 自动处理文本层提取，无需外部 OCR 服务。
 * <p>
 * 输出上限与降级语义由基类统一保证：
 * <ul>
 *   <li>超大 PDF 超过输出上限 → 返回部分内容 + "[文档过大，内容已截断]"；</li>
 *   <li>加密 / 损坏 / 解析失败 → 返回 "[文档解析失败]" 占位文案，
 *       异常不穿透到上传链路（与 ExcelExtractor 的"Tika 也失败则还原异常"不同，
 *       PDF 无备用本地解析器，占位文案即设计的终态——
 *       调用方可据此打 quality 标记决定是否转 MinerU 重解析）；</li>
 *   <li>扫描件（无文本层）→ 输出接近空串，同样建议由调用方转 OCR 链路。</li>
 * </ul>
 * filename 缺失时按文件头魔数嗅探（{@code %PDF-}）。
 *
 * @author mall
 */
@Component
public class PdfExtractor extends AbstractTikaExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractor.class);

    /** PDF 文件头魔数："%PDF-" */
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    /** 构造器注入：显式依赖、便于单测（PDF 路径不产图片，透传基类即可） */
    protected PdfExtractor(ImageExtractor imageExtractor) {
        super(imageExtractor);
    }

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String extract(FileSystemResource resource, String filename) throws Exception {
        File f = resource.getFile();
        if (filename == null && !looksLikePdf(f)) {
            // filename 缺失（supports 未被先调用的防御）且魔数不是 PDF：
            // 说明派发器路由错了，直接给出占位文案而非让 Tika 盲猜
            log.warn("dispatched non-pdf content to PdfExtractor: {}", f.getName());
            return "[文档解析失败]";
        }
        // 结构化 Markdown：标题/表格/列表保留；超限/失败降级语义见基类
        return parseTikaToMarkdown(f.toPath());
    }

    /** 按文件头魔数判断是否为 PDF（%PDF-，可能带偏移但前 1KB 内必出现，取前 1KB 嗅探） */
    private boolean looksLikePdf(File f) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
            byte[] head = in.readNBytes(1024);
            // 规范要求 %PDF- 在文件头，但容忍个别工具在头部加了 BOM/垃圾字节
            for (int i = 0; i + PDF_MAGIC.length <= head.length; i++) {
                if (matches(head, PDF_MAGIC, i)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;   // 读不出魔数时按非 PDF 处理，交由上层占位
        }
    }

    /** 从 offset 起比对魔数（容忍头部垃圾字节） */
    private static boolean matches(byte[] head, byte[] magic, int offset) {
        for (int i = 0; i < magic.length; i++) {
            if (head[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
