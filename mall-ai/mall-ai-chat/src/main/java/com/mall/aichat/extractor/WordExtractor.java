package com.mall.aichat.extractor;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.xml.namespace.QName;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word 文档提取器
 * <p>
 * docx 使用 POI 本地解析为 Markdown：
 * <ul>
 *   <li>按 BodyElementType 顺序遍历保持段落/表格原始顺序；</li>
 *   <li>用 XmlCursor 按"本地名"递归遍历 XML 子树（不依赖命名空间 URI、不使用 XPath）：
 *       支持 hyperlink / smartTag / sdt / fldSimple 内文本；过滤删除修订（del / delText）
 *       与移动修订旧位置（moveFrom）；AlternateContent 在段落级与 run 级均只取 Choice 分支
 *       （防止文本框/形状内容双份提取）；</li>
 *   <li>drawing/pict/object（含 OLE 嵌入对象）同时提取图片与文本框文字（收集全部 txbxContent，
 *       覆盖组合形状内的多个文本框），图片占位符就地产出（blip@embed / imagedata@id → rId → 图片），
 *       图文混排位置正确；</li>
 *   <li>标题识别四级匹配：styleId=headingN、数字 styleId（需样式名/outlineLvl 交叉验证）、
 *       样式名含"标题/heading N"、outlineLvl 大纲级别；另有无样式标题降级检测（全加粗+大字号+短文本）；</li>
 *   <li>项目符号/编号列表（numPr），缩进引用基类 {@link AbstractTikaExtractor#LIST_INDENT_UNIT}
 *       （每级 2 空格，全项目唯一权威定义——此位置历史上多次因合并事故回退，改前必须跑 golden 列表测试）；</li>
 *   <li>表格合并单元格填值（gridSpan / vMerge），单元格文本按实例缓存，
 *       vMerge continue 行不重复触发图片收集；单元格内嵌套表格转紧凑键值对；</li>
 *   <li>内嵌图片按内容 MD5 去重（收集时即去重，同图只存一份、只 OCR 一次），
 *       小图过滤，共享线程池并行 OCR，总超时保护；</li>
 *   <li>占位符使用私用区哨兵字符（\uE000...\uE001），不与正文碰撞、不受控制符清洗影响；</li>
 *   <li>内容控件（sdt）兜底取文本，页眉页脚丢弃。</li>
 * </ul>
 * doc（老格式，XWPF 不支持）回退基类 Tika 路径
 * （结构化 Markdown：标题/表格/列表保留；带输出上限，超限降级为部分内容，不让上传链路 OOM/500；
 * 输出质量仍低于 docx 路径，建议入库前过 DocumentCleaner 并打 quality=low 标记）。
 * filename 缺失时按文件头魔数嗅探（ZIP → .docx，否则 .doc）。
 *
 * @author mall
 */
@Component
public class WordExtractor extends AbstractTikaExtractor {

    private static final Logger log = LoggerFactory.getLogger(WordExtractor.class);

    private static final Pattern HEADING_ID_PATTERN = Pattern.compile("(?i)heading\\s*(\\d+)");
    private static final Pattern HEADING_NAME_PATTERN = Pattern.compile("(?i)(?:heading|标题)\\s*([1-9])");

    /**
     * 图片哨兵占位符：私用区字符 \uE000 / \uE001 包裹。
     * 私用区字符不会出现在正常正文中，也不会被 DocumentCleaner 的控制符规则（\x00-\x1F）删除。
     * 注意：图片哨兵必须在 DocumentCleaner 之前替换完成（当前实现满足此顺序）。
     */
    private static final String IMG_SENTINEL_PREFIX = "\uE000IMG";
    private static final String IMG_SENTINEL_SUFFIX = "\uE001";

    private static String sentinel(int id) { return IMG_SENTINEL_PREFIX + id + IMG_SENTINEL_SUFFIX; }

    private static final int MAX_HEADING_LEVEL = 6;            // Markdown 最多 6 级标题
    private static final int MAX_NESTED_TABLE_DEPTH = 2;       // 嵌套表格最大深度
    private static final int MIN_IMAGE_BYTES = 5 * 1024;       // 小于 5KB 的图（图标/分隔线）不 OCR
    private static final int OCR_THREADS = 4;                  // 共享 OCR 线程池大小
    private static final long OCR_TOTAL_TIMEOUT_SECONDS = 120; // 单文档图片批量 OCR 总超时
    private static final String PAGE_BREAK_MARK = "[分页]";

    /** ZIP 文件头魔数（.docx）：PK\x03\x04 */
    private static final byte[] ZIP_MAGIC = {'P', 'K', 0x03, 0x04};

    /** 共享 OCR 线程池：守护线程，随 JVM 退出；避免每文档创建销毁线程的开销。
     *  依赖 imageExtractor.extractImage 内部有单请求超时，防止坏图长期占用池线程。 */
    private static final ExecutorService IMAGE_OCR_POOL = Executors.newFixedThreadPool(
        OCR_THREADS, r -> {
            Thread t = new Thread(r, "docx-image-ocr");
            t.setDaemon(true);
            return t;
        });

    /** 图片提取总开关：批量灌库时可关闭以提速 */
    @Value("${extract.images:true}")
    private boolean extractImagesEnabled;

    /** 分页标记开关：开启后显式分页符处插入 [分页] 标记 */
    @Value("${extract.page-break-mark:false}")
    private boolean pageBreakMarkEnabled;

    /** 构造器注入：显式依赖、便于单测（imageExtractor 由基类持有） */
    protected WordExtractor(ImageExtractor imageExtractor) {
        super(imageExtractor);
    }

    // ==================================================================
    // 对外入口
    // ==================================================================

    @Override
    public boolean supports(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".doc") || lower.endsWith(".docx");
    }

    @Override
    public String extract(FileSystemResource resource, String filename) throws Exception {
        String lower = (filename == null) ? null : filename.toLowerCase();
        boolean isDocx;
        if (lower != null) {
            isDocx = lower.endsWith(".docx");
        } else {
            // filename 缺失（supports 未被先调用的防御）：按文件头魔数嗅探
            isDocx = looksLikeZip(resource.getFile());
        }
        if (isDocx) {
            return parseDocxWithPoi(resource.getFile());
        }
        // doc（老格式）：POI XWPF 不支持，回退基类 Tika 路径
        // （结构化 Markdown + 输出上限；超限/失败降级语义见基类）
        return parseTikaToMarkdown(resource.getFile().toPath());
    }

    /** 按文件头魔数判断是否为 zip 容器（docx），否则按 OLE2（doc）处理 */
    private boolean looksLikeZip(File f) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
            byte[] head = in.readNBytes(4);
            return matches(head, ZIP_MAGIC);
        } catch (Exception e) {
            return false;   // 读不出魔数时保守按 doc（Tika 路径对未知格式也能兜住）
        }
    }

    private static boolean matches(byte[] head, byte[] magic) {
        if (head.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (head[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    // ==================================================================
    // docx → Markdown 主流程
    // ==================================================================

    private String parseDocxWithPoi(File f) throws Exception {
        // 两层 try-with-resources，显式关闭 FileInputStream（XWPFDocument 不保证关闭传入流）
        try (FileInputStream fis = new FileInputStream(f);
             XWPFDocument doc = new XWPFDocument(fis)) {

            // 0. 预扫描：统计正文字号（用于无样式标题的降级检测）
            float bodySize = detectBodyFontSize(doc);

            StringBuilder md = new StringBuilder();
            ImageCollector ctx = new ImageCollector();

            // 1. 按 BodyElementType 顺序遍历，保持段落/表格原始顺序（不要分两个循环！）
            for (IBodyElement e : doc.getBodyElements()) {
                if (e instanceof XWPFParagraph p) {
                    appendParagraph(md, p, doc, bodySize, ctx);
                } else if (e instanceof XWPFTable t) {
                    String tableMd = tableToMarkdown(t, doc, ctx, 0);
                    if (!tableMd.isBlank()) {
                        md.append(tableMd).append("\n\n");
                    }
                } else if (e instanceof XWPFSDT sdt) {
                    // 内容控件（企业模板常见）：BodyElementType 遍历时会被静默跳过，这里兜底取文本
                    String text = sdt.getContent() != null ? sdt.getContent().getText() : null;
                    if (text != null && !text.isBlank()) {
                        md.append(text.strip()).append("\n\n");
                    }
                }
            }
            // 2. 图片哨兵 → OCR 识别结果
            return replaceImagePlaceholders(md.toString(), ctx);
        }
    }

    // ==================================================================
    // 段落处理
    // ==================================================================

    private void appendParagraph(StringBuilder md, XWPFParagraph p, XWPFDocument doc,
                                 float bodySize, ImageCollector ctx) {
        String text = cleanRevision(p, doc, ctx);
        boolean pageBreak = hasPageBreak(p);

        if (!text.isBlank()) {
            int level = headingLevel(doc, p);
            if (level <= 0 && isFallbackHeading(p, bodySize)) {
                // 无 Heading 样式但满足"全加粗 + 字号 >= 正文×1.15 + 短文本" → 视为二级标题
                level = 2;
            }
            if (level > 0) {
                md.append("#".repeat(Math.min(level, MAX_HEADING_LEVEL)))
                    .append(' ').append(text).append("\n\n");
            } else {
                md.append(listPrefix(p)).append(text).append("\n\n");
            }
        }
        if (pageBreak && pageBreakMarkEnabled) {
            md.append(PAGE_BREAK_MARK).append("\n\n");
        }
    }

    /**
     * 段落样式 → 标题级别，四级匹配策略（覆盖英文/中文/自定义标题样式）：
     * 1. styleId 形如 "headingN"（英文 Word 默认）；
     * 2. styleId 为 "1"~"9"（中文 Word 生成的标题样式 styleId 就是纯数字），
     *    需交叉验证：样式名匹配"标题/heading N"，或样式定义了 outlineLvl ——
     *    防止恰好叫 "1"~"9" 的自定义列表/表格样式被误判（宁缺勿滥）；
     * 3. 样式名称匹配 "heading N" / "标题 N"（覆盖其他语言变体）；
     * 4. 样式定义了 outlineLvl（0~8 对应 Heading1~9）——企业自定义标题样式的常见形态。
     */
    private int headingLevel(XWPFDocument doc, XWPFParagraph p) {
        String s = p.getStyleID();
        if (s == null) {
            return 0;
        }
        // 1. styleId = headingN
        Matcher m = HEADING_ID_PATTERN.matcher(s);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }

        XWPFStyles styles = doc.getStyles();
        XWPFStyle st = (styles != null) ? styles.getStyle(s) : null;
        String styleName = (st != null && st.getName() != null) ? st.getName() : null;
        Integer outlineLvl = extractOutlineLvl(st);

        // 2. 中文 Word：数字 styleId，需样式名或 outlineLvl 佐证
        if (s.matches("[1-9]")) {
            if (styleName != null && HEADING_NAME_PATTERN.matcher(styleName).find()) {
                return Integer.parseInt(s);
            }
            if (outlineLvl != null) {
                return outlineLvl + 1;
            }
            return 0;
        }
        // 3. 样式名称匹配（中文"标题 N"/英文变体）
        if (styleName != null) {
            Matcher nm = HEADING_NAME_PATTERN.matcher(styleName);
            if (nm.find()) {
                return Integer.parseInt(nm.group(1));
            }
        }
        // 4. outlineLvl 大纲级别（自定义标题样式最可靠的信号）
        if (outlineLvl != null) {
            return outlineLvl + 1;
        }
        return 0;
    }

    /** 读取样式定义的大纲级别 outlineLvl（0~8 对应 Heading1~9），无则返回 null */
    private Integer extractOutlineLvl(XWPFStyle st) {
        if (st == null || st.getCTStyle() == null
            || st.getCTStyle().getPPr() == null
            || st.getCTStyle().getPPr().getOutlineLvl() == null) {
            return null;
        }
        BigInteger v = st.getCTStyle().getPPr().getOutlineLvl().getVal();
        if (v == null) {
            return null;
        }
        int lvl = v.intValue();
        return (lvl >= 0 && lvl <= 8) ? lvl : null;
    }

    /**
     * 无样式标题降级检测：全加粗 + 字号 >= 正文×1.15 + 长度 <= 50 字符。
     * 用 getIRuns()（而非 getRuns()）：覆盖超链接/内容控件内的加粗标题 run。
     */
    private boolean isFallbackHeading(XWPFParagraph p, float bodySize) {
        String plain = p.getText();
        if (plain == null || plain.isBlank() || plain.length() > 50) {
            return false;
        }
        boolean anyText = false;
        boolean allBold = true;
        float maxSize = 0;
        for (IRunElement ire : p.getIRuns()) {
            if (!(ire instanceof XWPFRun r)) {
                continue;
            }
            if (r.text() == null || r.text().isBlank()) {
                continue;
            }
            anyText = true;
            if (!r.isBold()) {
                allBold = false;
            }
            int s = r.getFontSize();
            if (s > 0) {
                maxSize = Math.max(maxSize, s);
            }
        }
        return anyText && allBold && maxSize >= bodySize * 1.15f;
    }

    /** 项目符号/编号列表前缀（numPr）：缩进引用基类 {@link AbstractTikaExtractor#LIST_INDENT_UNIT}
     *  常量（全项目唯一权威定义，勿在本类复制副本）；编号实际值不解析，统一用 "- " */
    private String listPrefix(XWPFParagraph p) {
        CTP ctp = p.getCTP();
        if (ctp == null || ctp.getPPr() == null || ctp.getPPr().getNumPr() == null) {
            return "";
        }
        int lvl = 0;
        if (ctp.getPPr().getNumPr().getIlvl() != null) {
            BigInteger v = ctp.getPPr().getNumPr().getIlvl().getVal();
            if (v != null) {
                lvl = Math.min(v.intValue(), 5);
            }
        }
        return LIST_INDENT_UNIT.repeat(lvl) + "- ";
    }

    /** 是否包含显式分页符（run 内 br[type=page] 或段前分页 pageBreakBefore） */
    private boolean hasPageBreak(XWPFParagraph p) {
        CTP ctp = p.getCTP();
        if (ctp == null) {
            return false;
        }
        if (ctp.getPPr() != null && ctp.getPPr().getPageBreakBefore() != null) {
            return true;
        }
        // XmlCursor 遍历：段落 → run → br，检查 br 的本地属性 type 是否为 page
        XmlCursor cursor = ctp.newCursor();
        try {
            if (cursor.toFirstChild()) {
                do {
                    if (!"r".equals(localNameAt(cursor))) {
                        continue;
                    }
                    XmlCursor rc = cursor.newCursor();
                    try {
                        if (rc.toFirstChild()) {
                            do {
                                if ("br".equals(localNameAt(rc))
                                    && "page".equals(attrLocal(rc, "type"))) {
                                    return true;
                                }
                            } while (rc.toNextSibling());
                        }
                    } finally {
                        rc.dispose();
                    }
                } while (cursor.toNextSibling());
            }
            return false;
        } finally {
            cursor.dispose();
        }
    }

    // ==================================================================
    // 段落文本提取（修订过滤 + 超链接 + 图片/文本框就地提取，纯本地名遍历）
    // ==================================================================

    /**
     * 提取段落文本 + 就地产出图片哨兵：
     * 用 XmlCursor 按"本地名"递归遍历 CTP 子树，
     * - r：输出文本（t/tab/br/cr），遇到 drawing/pict/object 时：
     *      ① 解析 blip@embed / imagedata@id 的 rId，就地取出图片并产出哨兵；
     *      ② 收集全部 txbxContent（文本框/形状内的段落文字，组合形状有多个文本框）；
     *      遇到 AlternateContent（形状/文本框的标准包裹）时只取 Choice 分支；
     * - del / delText（删除修订）：整棵子树跳过（含其中的图片，符合"删除即不存在"语义）；
     * - moveFrom（移动修订旧位置）：整棵子树跳过（moveTo 处有新位置内容，跳过防止重复）；
     * - ins（插入修订）：递归取内容；
     * - hyperlink / smartTag / sdt / fldSimple 等容器：递归进入（超链接文本/图片不丢失）。
     */
    private String cleanRevision(XWPFParagraph p, XWPFDocument doc, ImageCollector ctx) {
        CTP ctp = p.getCTP();
        if (ctp == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        collectText(ctp, sb, doc, ctx);
        return sb.toString().trim();
    }

    /** 递归收集容器内的文本与图片，按本地名分发；不使用 XPath、不涉及命名空间 URI */
    private void collectText(XmlObject container, StringBuilder sb,
                             XWPFDocument doc, ImageCollector ctx) {
        XmlCursor cursor = container.newCursor();
        try {
            if (!cursor.toFirstChild()) {
                return;
            }
            do {
                String name = localNameAt(cursor);
                XmlObject child = cursor.getObject();
                switch (name) {
                    case "r" -> appendRunText(child, sb, doc, ctx);
                    case "del", "delText" -> { /* 删除修订：整棵子树跳过 */ }
                    case "moveFrom" -> { /* 移动修订旧位置：跳过，moveTo 处有新位置内容 */ }
                    // AlternateContent 的 Choice/Fallback 存同一内容两份，只取 Choice。
                    // 段落级与 run 级（appendRunText 内）都有此分支，两条路径均已覆盖。
                    case "AlternateContent" -> collectChoiceOnly(child, sb, doc, ctx);
                    // ins（插入修订）/ hyperlink / smartTag / sdt / fldSimple / 其他容器 → 递归
                    default -> collectText(child, sb, doc, ctx);
                }
            } while (cursor.toNextSibling());
        } finally {
            cursor.dispose();
        }
    }

    /** AlternateContent：仅进入第一个 Choice 子分支（若无 Choice 则整体跳过） */
    private void collectChoiceOnly(XmlObject alternateContent, StringBuilder sb,
                                   XWPFDocument doc, ImageCollector ctx) {
        XmlCursor cursor = alternateContent.newCursor();
        try {
            if (!cursor.toFirstChild()) {
                return;
            }
            do {
                if ("Choice".equals(localNameAt(cursor))) {
                    collectText(cursor.getObject(), sb, doc, ctx);
                    return;
                }
                // Requires / Fallback 等其他分支跳过
            } while (cursor.toNextSibling());
        } finally {
            cursor.dispose();
        }
    }

    /**
     * 提取单个 run 的文本内容（t/tab/br/cr）；
     * drawing（DrawingML）/ pict（VML）/ object（OLE 嵌入对象）：
     * 同时提取图片与文本框文字；
     * AlternateContent（run 级形状/文本框的标准包裹，Word 插入形状的默认产物）：
     * 只取 Choice 分支——缺失此分支会导致形状内容整体丢失且去重失效。
     */
    private void appendRunText(XmlObject run, StringBuilder sb,
                               XWPFDocument doc, ImageCollector ctx) {
        XmlCursor cursor = run.newCursor();
        try {
            if (!cursor.toFirstChild()) {
                return;
            }
            do {
                String name = localNameAt(cursor);
                switch (name) {
                    case "t" -> {
                        String v = ((CTText) cursor.getObject()).getStringValue();
                        if (v != null) {
                            sb.append(v);
                        }
                    }
                    case "tab" -> sb.append(' ');
                    case "br", "cr" -> sb.append('\n');
                    case "drawing", "pict" -> {
                        // ① 图片
                        appendPicture(cursor.getObject(), sb, doc, ctx);
                        // ② 文本框/形状内的文字（DrawingML 的 wps:txbx 与 VML 的 v:textbox
                        //    内部都是 w:txbxContent > w:p > w:r，复用 collectText 递归提取）
                        collectAllTxbxContent(cursor.getObject(), sb, doc, ctx);
                    }
                    // OLE 嵌入对象（Excel/公式/Visio）：图标是 v:shape > v:imagedata，取其图片
                    case "object" -> appendPicture(cursor.getObject(), sb, doc, ctx);
                    // run 级形状/文本框：mc:AlternateContent 包裹 Choice(drawing)/Fallback(pict)
                    case "AlternateContent" -> collectChoiceOnly(cursor.getObject(), sb, doc, ctx);
                    default -> { /* sym、noBreakHyphen 等非文本元素忽略 */ }
                }
            } while (cursor.toNextSibling());
        } finally {
            cursor.dispose();
        }
    }

    /**
     * 收集 drawing/pict 子树中全部 txbxContent（文本框内容容器）并递归提取其中段落文本。
     * 不做"找到即返回"：组合形状（group，多个形状成组）内每个形状都有自己的 txbxContent，
     * 只取第一个会丢失其余形状的文字。普通形状只有一个 txbx，行为不变。
     * 不会重复提取：AlternateContent 已只走 Choice（drawing 那份），
     * Fallback（pict 那份）被跳过；裸 pict（老文档）只出现一次。
     */
    private void collectAllTxbxContent(XmlObject root, StringBuilder sb,
                                       XWPFDocument doc, ImageCollector ctx) {
        XmlCursor cursor = root.newCursor();
        try {
            if (!cursor.toFirstChild()) {
                return;
            }
            do {
                if ("txbxContent".equals(localNameAt(cursor))) {
                    sb.append(' ');
                    collectText(cursor.getObject(), sb, doc, ctx);
                } else {
                    collectAllTxbxContent(cursor.getObject(), sb, doc, ctx);
                }
            } while (cursor.toNextSibling());
        } finally {
            cursor.dispose();
        }
    }

    /**
     * 就地提取图片：在子树中查找图片关系 rId ——
     * DrawingML 的 blip@embed，或 VML 的 imagedata@id；
     * 通过 doc.getPictureDataByID(rId) 取得图片数据后产出哨兵占位符。
     * 外链图片（blip@link，不打包进 docx）记 debug 日志后跳过——本来也无法 OCR。
     */
    private void appendPicture(XmlObject drawingOrPictOrObject, StringBuilder sb,
                               XWPFDocument doc, ImageCollector ctx) {
        String relId = findPictureRelId(drawingOrPictOrObject);
        if (relId == null) {
            return;
        }
        XWPFPictureData data = doc.getPictureDataByID(relId);
        if (data != null) {
            int id = ctx.add(data);
            sb.append(sentinel(id));
        }
    }

    /**
     * 在图片元素子树中查找关系 rId：
     * - 元素本地名为 blip → 优先属性 embed；仅 link 时为外链图，记日志返回 null
     * - 元素本地名为 imagedata → 属性 id
     * 只比较本地名，不涉及命名空间 URI。
     */
    private String findPictureRelId(XmlObject root) {
        XmlCursor cursor = root.newCursor();
        try {
            if (!cursor.toFirstChild()) {
                return null;
            }
            do {
                String elName = localNameAt(cursor);
                if ("blip".equals(elName)) {
                    String id = attrLocal(cursor, "embed");
                    if (id != null) {
                        return id;
                    }
                    if (attrLocal(cursor, "link") != null) {
                        log.debug("skip external linked image (r:link), not embedded in docx");
                    }
                } else if ("imagedata".equals(elName)) {
                    String id = attrLocal(cursor, "id");
                    if (id != null) {
                        return id;
                    }
                }
                // 递归子树
                String nested = findPictureRelId(cursor.getObject());
                if (nested != null) {
                    return nested;
                }
            } while (cursor.toNextSibling());
            return null;
        } finally {
            cursor.dispose();
        }
    }

    // ==================================================================
    // XmlCursor 本地名工具（全部只比较 localPart，不涉及命名空间 URI）
    // ==================================================================

    /** 当前 cursor 所指节点的本地名（无命名空间前缀） */
    private static String localNameAt(XmlCursor c) {
        QName q = c.getName();
        return q != null ? q.getLocalPart() : "";
    }

    /** 在当前节点的属性里按本地名取值（不比较命名空间），不存在返回 null */
    private static String attrLocal(XmlCursor c, String localName) {
        c.push();
        try {
            if (c.toFirstAttribute()) {
                do {
                    QName q = c.getName();
                    if (q != null && localName.equals(q.getLocalPart())) {
                        return c.getTextValue();
                    }
                } while (c.toNextAttribute());
            }
            return null;
        } finally {
            c.pop();
        }
    }

    // ==================================================================
    // 表格处理（gridSpan / vMerge / 嵌套表格键值化 / 单元格缓存）
    // ==================================================================

    private String tableToMarkdown(XWPFTable table, XWPFDocument doc,
                                   ImageCollector ctx, int depth) {
        if (depth > MAX_NESTED_TABLE_DEPTH) {
            return "[嵌套表格略]";
        }
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return "";
        }

        // 第一遍统一提取所有单元格文本并按实例缓存。
        // 后续组装（含 vMerge continue 行回填起始单元格）只查缓存，
        // 绝不重新提取 → 图片不会被重复收集、不会被重复 OCR。
        Map<XWPFTableCell, String> cache = new IdentityHashMap<>();
        for (XWPFTableRow row : rows) {
            for (XWPFTableCell cell : row.getTableCells()) {
                cache.put(cell, cellText(cell, doc, ctx, depth));
            }
        }

        // 计算最大列数（考虑 gridSpan）
        int maxCols = 0;
        for (XWPFTableRow row : rows) {
            int cols = 0;
            for (XWPFTableCell cell : row.getTableCells()) {
                cols += getGridSpan(cell);
            }
            maxCols = Math.max(maxCols, cols);
        }
        if (maxCols == 0) {
            return "";
        }

        // 纵向合并起始行（vMerge restart）
        Map<Integer, Integer> vMergeStart = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);
            int colIdx = 0;
            List<String> lineCells = new ArrayList<>();

            for (XWPFTableCell cell : row.getTableCells()) {
                int span = getGridSpan(cell);
                String cellText;
                String vMerge = getVMergeVal(cell);

                if ("restart".equals(vMerge)) {
                    vMergeStart.put(colIdx, r);
                    cellText = cache.get(cell);
                } else if ("continue".equals(vMerge)) {
                    // 合并单元格：填起始行的值（按物理列定位起始 cell，再用缓存取文本）
                    Integer startRow = vMergeStart.get(colIdx);
                    XWPFTableCell src = startRow != null
                        ? cellAtColumn(rows.get(startRow), colIdx) : null;
                    cellText = (src != null && cache.containsKey(src))
                        ? cache.get(src) : cache.get(cell);
                } else {
                    cellText = cache.get(cell);
                }

                lineCells.add(sanitizeCell(cellText));
                colIdx += span;
            }

            // 补齐缺失列
            while (lineCells.size() < maxCols) {
                lineCells.add("");
            }

            sb.append("| ").append(String.join(" | ", lineCells)).append(" |\n");
            if (r == 0) {
                sb.append("| ").append(String.join(" | ", Collections.nCopies(maxCols, "---")))
                    .append(" |\n");
            }
        }
        return sb.toString();
    }

    /** 按物理列号定位行内单元格（考虑 gridSpan），找不到返回 null */
    private XWPFTableCell cellAtColumn(XWPFTableRow row, int targetCol) {
        int cur = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            int span = getGridSpan(cell);
            if (targetCol >= cur && targetCol < cur + span) {
                return cell;
            }
            cur += span;
        }
        return null;
    }

    /**
     * 单元格文本：段落 + 嵌套表格（嵌套表转紧凑键值对，
     * 避免"管道乱码"——展平的 | a | b | 在 Markdown 单元格里不可读、检索语义差）。
     */
    private String cellText(XWPFTableCell cell, XWPFDocument doc, ImageCollector ctx, int depth) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : cell.getParagraphs()) {
            String t = cleanRevision(p, doc, ctx);
            if (!t.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(t.strip());
            }
        }
        for (XWPFTable nested : cell.getTables()) {
            String t = compactTable(nested, doc, ctx, depth + 1);
            if (!t.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(t);
            }
        }
        return sb.toString();
    }

    /**
     * 嵌套表转紧凑键值对：以首行为表头，其余行 "表头:值; 表头:值"，
     * 单行表格则直接输出行文本。按 gridSpan 累计列索引，保证与表头对位。
     */
    private String compactTable(XWPFTable t, XWPFDocument doc, ImageCollector ctx, int depth) {
        if (depth > MAX_NESTED_TABLE_DEPTH) {
            return "";
        }
        List<XWPFTableRow> rows = t.getRows();
        if (rows.isEmpty()) {
            return "";
        }
        if (rows.size() == 1) {
            StringBuilder sb = new StringBuilder();
            for (XWPFTableCell c : rows.get(0).getTableCells()) {
                String v = cellText(c, doc, ctx, depth);
                if (!v.isBlank()) {
                    if (!sb.isEmpty()) sb.append(' ');
                    sb.append(v.strip());
                }
            }
            return sb.toString();
        }
        // 首行为表头
        List<String> headers = new ArrayList<>();
        for (XWPFTableCell c : rows.get(0).getTableCells()) {
            headers.add(cellText(c, doc, ctx, depth).strip());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < rows.size(); i++) {
            int colIdx = 0;
            for (XWPFTableCell c : rows.get(i).getTableCells()) {
                String key = colIdx < headers.size() ? headers.get(colIdx) : "";
                String val = cellText(c, doc, ctx, depth).strip();
                if (!val.isBlank()) {
                    if (!sb.isEmpty()) {
                        sb.append("; ");
                    }
                    sb.append(key.isBlank() ? "项" : key).append(':').append(val);
                }
                colIdx += getGridSpan(c);   // 累计物理列，保证与表头对位
            }
        }
        return sb.toString();
    }

    /** 单元格内容净化：换行压成空格、竖线转义，防止破坏 Markdown 表格结构 */
    private String sanitizeCell(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\r\\n]+", " ")
            .replace("|", "\\|")
            .trim();
    }

    /** 横向合并跨度（gridSpan） */
    private int getGridSpan(XWPFTableCell cell) {
        var tcPr = cell.getCTTc().getTcPr();
        if (tcPr != null && tcPr.getGridSpan() != null) {
            return tcPr.getGridSpan().getVal().intValue();
        }
        return 1;
    }

    /** 纵向合并类型（vMerge）：restart / continue / null */
    private String getVMergeVal(XWPFTableCell cell) {
        var tcPr = cell.getCTTc().getTcPr();
        if (tcPr != null && tcPr.getVMerge() != null) {
            var val = tcPr.getVMerge().getVal();
            return val != null ? val.toString() : "continue";
        }
        return null;
    }

    // ==================================================================
    // 正文基准字号检测（无样式标题降级检测的基准）
    // ==================================================================

    private float detectBodyFontSize(XWPFDocument doc) {
        Map<Integer, Integer> freq = new HashMap<>();
        collectFontFreq(doc.getParagraphs(), freq);
        for (XWPFTable t : doc.getTables()) {
            for (XWPFTableRow row : t.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    collectFontFreq(cell.getParagraphs(), freq);
                }
            }
        }
        if (!freq.isEmpty()) {
            return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() / 2f)   // 收集时 ×2 保留半磅精度
                .orElse(10.5f);
        }
        // 兜底：run 级无显式字号时（字号定义在样式层），尝试默认样式
        try {
            XWPFStyles styles = doc.getStyles();
            if (styles != null) {
                XWPFStyle normal = styles.getStyle("Normal");
                if (normal != null && normal.getCTStyle() != null
                    && normal.getCTStyle().getRPr() != null
                    && normal.getCTStyle().getRPr().getSz() != null) {
                    return normal.getCTStyle().getRPr().getSz().getVal().intValue() / 2f;
                }
            }
        } catch (Exception e) {
            log.debug("read default font size failed, use fallback", e);
        }
        return 10.5f;
    }

    private void collectFontFreq(List<XWPFParagraph> paragraphs, Map<Integer, Integer> freq) {
        for (XWPFParagraph p : paragraphs) {
            for (XWPFRun r : p.getRuns()) {
                int s = r.getFontSize();
                if (s > 0) {
                    freq.merge(s * 2, 1, Integer::sum);
                }
            }
        }
    }

    // ==================================================================
    // 图片收集（收集时即按 MD5 去重：同图只存一份、只占一个哨兵位）
    // ==================================================================

    /** 图片收集上下文：内容去重 + 哈希 → 占位符编号映射 */
    static final class ImageCollector {
        /** MD5 → 占位符编号（去重映射） */
        private final Map<String, Integer> hashToId = new LinkedHashMap<>();
        /** 去重后的图片数据（同图只存一份，降低内存驻留） */
        final List<ImageItem> images = new ArrayList<>();

        /** 添加图片：重复图返回已有占位符编号，不新增存储 */
        int add(XWPFPictureData data) {
            byte[] bytes = data.getData();
            String hash = DigestUtils.md5DigestAsHex(bytes);
            Integer existing = hashToId.get(hash);
            if (existing != null) {
                return existing;
            }
            String ext = data.suggestFileExtension();   // 真实扩展名，不写死 png
            if (ext == null || ext.isBlank()) {
                ext = "png";
            }
            images.add(new ImageItem(bytes, ext, hash));
            int id = images.size() - 1;
            hashToId.put(hash, id);
            return id;
        }

        record ImageItem(byte[] data, String ext, String hash) {}
    }

    // ==================================================================
    // 图片 OCR：去重已完成 → 小图过滤 → 共享池并行识别 → 哨兵回填
    // ==================================================================

    private String replaceImagePlaceholders(String markdown, ImageCollector ctx) {
        List<ImageCollector.ImageItem> images = ctx.images;
        if (images.isEmpty()) {
            return markdown;
        }

        String[] fills = new String[images.size()];
        if (!extractImagesEnabled) {
            // 总开关关闭：占位符直接清空，不调 OCR
            Arrays.fill(fills, "");
        } else {
            // 1. 过滤小图：图标/分隔线不值得 OCR（去重已在收集时完成）
            List<ImageCollector.ImageItem> toOcr = images.stream()
                .filter(item -> item.data().length >= MIN_IMAGE_BYTES)
                .toList();

            // 2. 并行 OCR（类级共享池 + 总超时，防止一张坏图拖垮整个上传链路）
            Map<String, String> ocrByHash = new ConcurrentHashMap<>();
            if (!toOcr.isEmpty()) {
                try {
                    List<CompletableFuture<Void>> futures = toOcr.stream()
                        .map(item -> CompletableFuture.runAsync(() -> {
                            try {
                                String text = imageExtractor.extractImage(
                                    new ByteArrayResource(item.data()),
                                    "image-" + item.hash().substring(0, 8) + "." + item.ext());
                                ocrByHash.put(item.hash(),
                                    (text == null || text.isBlank())
                                        ? "[图片内容为空]" : text);
                            } catch (Exception e) {
                                log.warn("image OCR failed, hash={}", item.hash(), e);
                                ocrByHash.put(item.hash(), "[图片解析失败]");
                            }
                        }, IMAGE_OCR_POOL))
                        .toList();
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(OCR_TOTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("batch image OCR timeout or interrupted", e);
                    for (ImageCollector.ImageItem item : toOcr) {
                        ocrByHash.putIfAbsent(item.hash(), "[图片解析超时]");
                    }
                }
            }

            // 3. 生成每个占位符的回填内容（同哈希共享同一结果；小图直接清空，不产出噪音）
            for (int i = 0; i < images.size(); i++) {
                ImageCollector.ImageItem item = images.get(i);
                fills[i] = item.data().length < MIN_IMAGE_BYTES
                    ? "" : ocrByHash.getOrDefault(item.hash(), "[图片解析失败]");
            }
        }

        // 4. 哨兵替换（私用区哨兵，不与正文碰撞、不受控制符清洗影响）
        StringBuilder result = new StringBuilder(markdown);
        for (int i = 0; i < images.size(); i++) {
            String token = sentinel(i);
            int pos = result.indexOf(token);
            if (pos >= 0) {
                String rep = fills[i].isBlank() ? "" : "\n[图片说明: " + fills[i] + "]\n";
                result.replace(pos, pos + token.length(), rep);
            }
        }
        // 保险：清理任何残留哨兵（理论上不会发生）
        int p;
        while ((p = result.indexOf(IMG_SENTINEL_PREFIX)) >= 0) {
            int end = result.indexOf(IMG_SENTINEL_SUFFIX, p + IMG_SENTINEL_PREFIX.length());
            result.replace(p, end >= 0 ? end + 1 : result.length(), "");
        }
        return result.toString();
    }
}
