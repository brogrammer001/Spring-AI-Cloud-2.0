package com.mall.aichat.extractor;

import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.WriteOutContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 基于 Apache Tika 的通用文档提取器抽象基类
 * <p>
 * 供 {@link PdfExtractor} 与 {@link WordExtractor}（doc 老格式回退）复用，提供两条受控解析路径：
 * <ul>
 *   <li>{@link #parseWithTikaLimited(Path)}：纯文本输出（带输出上限），作为兜底路径；</li>
 *   <li>{@link #parseTikaToMarkdown(Path)}：XHTML SAX 事件 → 流式转 Markdown，
 *       保留标题/表格/列表结构，PDF 与 .doc 均可用（优于默认纯文本）。</li>
 * </ul>
 * <p>
 * 超限（writeLimit）与解析失败语义分开：超限返回部分内容 + 截断标注；
 * 失败返回"[文档解析失败]"占位文案。均不向上抛异常——调用方（上传链路）拿到的是可用字符串。
 * <p>
 * 注意：不使用 Spring AI 的 {@code TikaDocumentReader}——其内部 handler 无 writeLimit，
 * 大文档会全文解入内存导致 OOM，且拿不到内部 handler 无法加限制。
 *
 * <b>依赖收敛警告</b>：tika 传递依赖 poi/poi-ooxml/poi-scratchpad/xmlbeans，
 * 必须与项目显式 POI 版本用 dependencyManagement 收敛到单一版本，否则 POI 路径
 * 可能在特定文档上抛 NoSuchMethodError。上线前执行：
 * {@code mvn dependency:tree -Dincludes=org.apache.poi,org.apache.xmlbeans,org.apache.tika}
 *
 * @author mall
 */
public abstract class AbstractTikaExtractor implements Extractor {

    private static final Logger log = LoggerFactory.getLogger(AbstractTikaExtractor.class);

    /**
     * 列表缩进单元：Markdown 嵌套列表要求每级至少 2 空格（1 空格会导致二级以后的列表被当成正文）。
     * <p>
     * ⚠️ 全项目唯一权威定义（WordExtractor 的 docx 路径与 {@link XhtmlToMarkdownHandler}
     * 均引用此常量，勿再复制副本——历史上存在过多份独立副本各自回退成单空格的事故）。
     * 禁止直接改动；改动前必须跑 golden 列表测试：三级列表断言缩进为 0/2/4 空格，
     * 且不出现 "(?m)^ - " 模式。
     */
    public static final String LIST_INDENT_UNIT = "  ";

    /**
     * Tika 解析输出上限（字符数）：超过此值降级为部分内容，防止全文解入内存 OOM。
     * 2M 字符 ≈ 4MB String 内存，可按容器内存调整（配置项 extract.tika-max-chars）。
     */
    @Value("${extract.tika-max-chars:2097152}")
    private int tikaMaxChars;

    protected final ImageExtractor imageExtractor;

    /** 构造器注入：显式依赖、便于单测（子类透传） */
    protected AbstractTikaExtractor(ImageExtractor imageExtractor) {
        this.imageExtractor = imageExtractor;
    }

    /** Tika 解析结果：正常 / 输出超限 / 解析失败 */
    private enum TikaResult { OK, LIMIT, ERROR }

    // ==================================================================
    // 路径一：纯文本 + 输出上限（兜底）
    // ==================================================================

    /**
     * Tika 解析为纯文本（带输出上限）。
     * 超限返回已积累的部分内容 + 截断标注；失败返回错误占位文案。不向上抛异常。
     */
    protected String parseWithTikaLimited(Path file) {
        TextContentHandler handler = new TextContentHandler(tikaMaxChars);
        TikaResult result = runTika(file, handler);
        return degrade(result, handler.text());
    }

    // ==================================================================
    // 路径二：结构化 Markdown（PDF / .doc 共用，优于纯文本）
    // ==================================================================

    /**
     * Tika 解析为结构化 Markdown：
     * 内置 {@link XhtmlToMarkdownHandler} 接收 XHTML SAX 事件流式转换
     * （标题/表格/列表结构保留）。超限/失败的降级语义见
     * {@link #parseWithTikaLimited(Path)}。
     */
    protected String parseTikaToMarkdown(Path file) {
        XhtmlToMarkdownHandler mdHandler = new XhtmlToMarkdownHandler(tikaMaxChars);
        TikaResult result = runTika(file, mdHandler);
        return degrade(result, mdHandler.markdown());
    }

    /**
     * 按解析结果组装输出：
     * - OK → 全量内容；
     * - LIMIT → 部分内容 + 截断标注（空内容时给占位文案）；
     * - ERROR → 错误占位文案（不误标"截断"，便于排障区分）。
     */
    private String degrade(TikaResult result, String content) {
        String trimmed = content.trim();
        return switch (result) {
            case OK -> trimmed;
            case LIMIT -> {
                if (trimmed.isBlank()) {
                    yield "[文档解析失败]";
                }
                yield trimmed + "\n\n[文档过大，内容已截断]";
            }
            default -> "[文档解析失败]";
        };
    }

    /**
     * 执行 Tika 解析：WriteOutContentHandler 设输出上限。
     * Tika 2.0+ 超限时内部抛 {@link WriteLimitReachedException}（SAXException 子类），
     * 用其静态方法 {@code isWriteLimitReached(Throwable)}（遍历 cause 链）判别，
     * 而非 1.x 时代的 WriteOutContentHandler 实例方法（2.x 已移除）。
     *
     * @return OK 正常完成；LIMIT 输出超限；ERROR 解析失败（内容均已在 handler 内可取）
     */
    private TikaResult runTika(Path file, DefaultHandler contentHandler) {
        // WriteOutContentHandler 包一层：超限时抛 WriteLimitReachedException
        WriteOutContentHandler limitHandler =
            new WriteOutContentHandler(contentHandler, tikaMaxChars);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY,
                file.getFileName() != null ? file.getFileName().toString() : "");
            parser.parse(in, limitHandler, metadata, new ParseContext());
            return TikaResult.OK;
        } catch (SAXException e) {
            if (WriteLimitReachedException.isWriteLimitReached(e)) {
                log.warn("tika output limit reached ({} chars): {}", tikaMaxChars, file);
                return TikaResult.LIMIT;
            }
            log.warn("tika SAX error: {}", file, e);
            return TikaResult.ERROR;
        } catch (Exception e) {
            log.error("tika parse failed: {}", file, e);
            return TikaResult.ERROR;
        }
    }

    // ==================================================================
    // 内容收集 handler 们
    // ==================================================================

    /** 纯文本收集：仅累积 characters，供兜底路径使用 */
    private static final class TextContentHandler extends DefaultHandler {
        private final StringBuilder sb = new StringBuilder();
        private final int limit;

        TextContentHandler(int limit) {
            this.limit = limit;
        }

        String text() {
            return sb.toString();
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (sb.length() < limit) {          // 双保险：handler 层也不再无限追加
                sb.append(ch, start, length);
            }
        }
    }

    /**
     * 流式 XHTML → Markdown 转换 handler（覆盖 PDF 与 .doc 的常见结构）：
     * - h1~h6 → # 标题（最多 6 级）；单元格内出现的标题降级为纯文本；
     * - p → 段落（空段跳过）；单元格内出现的段落并入单元格文本；
     * - table/tr/td/th → Markdown 管道表格：单元格文本进 cellBuf → td 结束收进
     *   rowCells → tr 结束整行输出（首行后自动补 "---" 分隔行，单元格竖线转义）；
     *   嵌套表（tableDepth > 1）只取文本、不输出嵌套管道结构；
     * - ul/ol + li → 无序列表（每级 2 空格缩进，引用基类 {@link #LIST_INDENT_UNIT}，
     *   与 WordExtractor docx 路径一致——嵌套列表低于 2 空格渲染会失效）；
     * - br → 空格（表格内）/ 换行（表格外的 br 在 buf 中保留换行语义由 clean 压缩）；
     * - 其他元素（div/span 等）默认透传文本。
     */
    private static final class XhtmlToMarkdownHandler extends DefaultHandler {
        private static final int MAX_HEADING_LEVEL = 6;

        private final StringBuilder md = new StringBuilder();
        private final int limit;
        private final Deque<String> elements = new ArrayDeque<>();
        /** 表格外文本缓冲 */
        private final StringBuilder buf = new StringBuilder();
        /** 表格单元格文本缓冲（与 buf 分离，防止嵌套 table/p 的 flushBlock 丢内容） */
        private final StringBuilder cellBuf = new StringBuilder();
        /** 当前行已完成的单元格文本（tr 结束时组装输出） */
        private final List<String> rowCells = new ArrayList<>();
        private int listDepth = 0;
        /** 嵌套深度：1 = 最外层表格；>1 = 嵌套表（只取文本） */
        private int tableDepth = 0;
        private boolean firstRowOfTable = false;

        XhtmlToMarkdownHandler(int limit) {
            this.limit = limit;
        }

        String markdown() {
            flushBlock();
            return md.toString();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            String tag = localName.toLowerCase();
            elements.push(tag);
            switch (tag) {
                case "h1", "h2", "h3", "h4", "h5", "h6", "p", "li" -> {
                    if (tableDepth == 0) {
                        flushBlock();               // 块级结构前落盘残留缓冲；表内不做（会切断单元格）
                    }
                }
                case "table" -> {
                    if (tableDepth == 0) {
                        flushBlock();
                        firstRowOfTable = true;
                    }
                    tableDepth++;                   // 嵌套表：文本继续进 cellBuf，不产出嵌套结构
                }
                case "tr" -> rowCells.clear();
                case "td", "th" -> cellBuf.setLength(0);
                case "ul", "ol" -> {
                    if (tableDepth == 0) {
                        flushBlock();
                        listDepth++;
                    }
                }
                case "br" -> {
                    // 表格内 br 压成空格（单元格内容单行化）；表外保留换行（clean 时统一压缩）
                    (tableDepth > 0 ? cellBuf : buf).append(tableDepth > 0 ? " " : "\n");
                }
                default -> { }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String tag = elements.pop();
            switch (tag) {
                case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    if (tableDepth > 0) {
                        // 单元格内标题：降级为纯文本并入单元格
                        cellBuf.append(' ').append(clean(buf));
                        buf.setLength(0);
                        break;
                    }
                    md.append("#".repeat(Math.min(tag.charAt(1) - '0', MAX_HEADING_LEVEL)))
                        .append(' ').append(clean(buf)).append("\n\n");
                    buf.setLength(0);
                }
                case "p" -> {
                    if (tableDepth > 0) {
                        // 单元格内段落：并入单元格文本（空格分隔），不产段落结构
                        String t = clean(buf);
                        if (!t.isBlank()) {
                            cellBuf.append(cellBuf.isEmpty() ? "" : " ").append(t);
                        }
                        buf.setLength(0);
                        break;
                    }
                    String t = clean(buf);
                    if (!t.isBlank()) {
                        md.append(t).append("\n\n");
                    }
                    buf.setLength(0);
                }
                case "td", "th" -> {
                    // 单元格结束：内容收进 rowCells（此前版本在此处清空导致内容全部丢失）
                    rowCells.add(clean(tableDepth > 1 ? buf : cellBuf));
                    cellBuf.setLength(0);
                    buf.setLength(0);
                }
                case "tr" -> {
                    if (tableDepth == 1 && !rowCells.isEmpty()) {
                        md.append("| ").append(String.join(" | ", rowCells)).append(" |\n");
                        if (firstRowOfTable) {
                            md.append("| ").append(String.join(" | ",
                                Collections.nCopies(rowCells.size(), "---"))).append(" |\n");
                            firstRowOfTable = false;
                        }
                    }
                    rowCells.clear();
                }
                case "table" -> {
                    tableDepth--;
                    if (tableDepth == 0) {
                        inTableEnd();
                    }
                }
                case "ul", "ol" -> {
                    if (tableDepth > 0) {
                        break;
                    }
                    if (listDepth > 0) {
                        listDepth--;
                    }
                    if (listDepth == 0) {
                        md.append('\n');
                    }
                }
                case "li" -> {
                    if (tableDepth > 0) {
                        buf.setLength(0);
                        break;
                    }
                    String t = clean(buf);
                    if (!t.isBlank()) {
                        md.append(LIST_INDENT_UNIT.repeat(Math.min(Math.max(listDepth - 1, 0), 5)))
                            .append("- ").append(t).append('\n');
                    }
                    buf.setLength(0);
                }
                default -> { }
            }
        }

        private void inTableEnd() {
            md.append('\n');
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (md.length() + buf.length() + cellBuf.length() >= limit) {
                return;                                  // 双保险：handler 层不再无限追加
            }
            String s = new String(ch, start, length).replaceAll("\\s+", " ");
            // 按是否在表格内路由到正确缓冲（tr 元素本身无直接文本，无需分支）
            (tableDepth > 0 ? cellBuf : buf).append(s);
        }

        private String clean(StringBuilder b) {
            return b.toString().replaceAll("\\s+", " ").replace("|", "\\|").trim();
        }

        /** 未在结构化元素中的残留缓冲，按段落落盘（防止丢文本） */
        private void flushBlock() {
            String t = clean(buf);
            if (!t.isBlank()) {
                md.append(t).append("\n\n");
            }
            buf.setLength(0);
        }
    }
}
