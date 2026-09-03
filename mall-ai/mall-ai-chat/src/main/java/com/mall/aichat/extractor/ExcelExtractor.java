package com.mall.aichat.extractor;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Excel 文档提取器（改进版 v3）
 * <p>
 * 按文件类型/大小分流解析为 Markdown 表格：
 * <ul>
 *   <li>小文件（&lt;10MB）及 xls/xlsm：POI WorkbookFactory 全量解析，
 *       {@link DataFormatter} + {@link FormulaEvaluator} 按显示格式取值
 *       （避免日期/数字乱码、公式缓存失效）；合并单元格先填满区域值再输出
 *       （不存在的 cell 会 createCell 补齐，左上角为空值时跳过填值）；
 *       隐藏 sheet 跳过；第一个非空行当表头（前几行说明文字 + 表格在后很常见，
 *       首行全空时不能用 rowNum==0 判断）；</li>
 *   <li>大 xlsx（≥10MB）：FastExcel 流式解析（SAX），逐行读取不加载全表避免 OOM；
 *       按 sheet 名切分输出；表头行全空（invokeHeadMap 不回调）的 sheet
 *       用 headerPending 延迟表头——以首个非空数据行定列数并当表头，
 *       防止数据列被截断到 1 列；列数取表头最大物理列索引+1
 *       （headMap 的 key 稀疏时 size() 不可靠）；空数据行不占行数限额；</li>
 *   <li>csv：字符流逐行解析（RFC4180 状态机：引号内换行续读、双引号转义、
 *       引号内字段不 trim、字段内裸引号宽容处理——cur 为空才进引号模式，
 *       否则保留原字符，修复 ab"cd,1 逗号失效 + 吞行）；编码自动嗅探
 *       （UTF-8/UTF-16 BOM → 严格 UTF-8 校验（裁掉 8KB 边界切断的多字节尾部）→
 *       GB18030 回退）；剥离 BOM；数据行按表头列数补齐/截断；</li>
 *   <li>sheet 名写入 Markdown 标题；单 sheet 行数超限截断并标注；</li>
 *   <li>filename 缺失时按文件头魔数嗅探分流（OLE2 → xls/xlsm，ZIP → xlsx，
 *       可打印/UTF-8 文本 → csv）；主路径全部失败时 Tika 兜底
 *       （纯文本 + 输出上限），彻底消除上传链路 500；Tika 也失败时
 *       还原原始异常让上层走既有错误处理。</li>
 * </ul>
 *
 * @author mall
 */
@Component
public class ExcelExtractor extends AbstractTikaExtractor {

    private static final Logger log = LoggerFactory.getLogger(ExcelExtractor.class);

    /** 流式解析阈值：超过 10MB 走 FastExcel 流式，避免 POI 全量加载 OOM */
    private static final long STREAMING_THRESHOLD = 10 * 1024 * 1024L;

    /** 单元格最大字符数：超长截断，控制单行体积 */
    private static final int MAX_CELL_CHARS = 1000;

    /** 单 sheet 最大数据行数：超出截断并标注，防止百万行表产出超大 Markdown */
    private static final int MAX_ROWS_PER_SHEET = 10_000;

    /** GB18030：国内 Excel 导出 CSV 的常见编码（GBK 的超集） */
    private static final Charset GB18030 = Charset.forName("GB18030");

    /** OLE2 文件头魔数（xls/xlsm）：D0 CF 11 E0 */
    private static final byte[] OLE2_MAGIC = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};
    /** ZIP 文件头魔数（xlsx/xlsm）：PK\x03\x04 */
    private static final byte[] ZIP_MAGIC = {'P', 'K', 0x03, 0x04};

    /** 构造器注入：显式依赖、便于单测（Excel 路径不产图片，透传基类即可） */
    protected ExcelExtractor(ImageExtractor imageExtractor) {
        super(imageExtractor);
    }

    @Override
    public boolean supports(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        // .xlsm（宏工作簿）WorkbookFactory 原生支持，企业模板常见，不支持会落到 MinerU
        return lower.endsWith(".xls") || lower.endsWith(".xlsx")
            || lower.endsWith(".xlsm") || lower.endsWith(".csv");
    }

    @Override
    public String extract(FileSystemResource resource, String filename) throws Exception {
        File f = resource.getFile();
        String lower = (filename == null) ? null : filename.toLowerCase();
        if (lower == null) {
            // filename 缺失（supports 未被先调用的防御）：按文件头魔数嗅探分流
            PathKind kind = sniffPathKind(f);
            if (kind == PathKind.CSV) {
                return parseCsv(f);
            }
            // OLE2/ZIP/未知二进制：走 POI 全量（WorkbookFactory 通吃 xls/xlsx/xlsm）
            return parseNormalWithFallback(f);
        }

        try {
            if (lower.endsWith(".csv")) {
                return parseCsv(f);                                   // CSV：字符流逐行解析
            }
            if (lower.endsWith(".xlsx") && f.length() > STREAMING_THRESHOLD) {
                return parseStreaming(f);                             // 大 xlsx：FastExcel 流式
            }
            return parseNormal(f);                                    // 小文件 / xls / xlsm / 未知：POI 全量
        } catch (Exception e) {
            // 主路径失败（损坏文件 / 密码保护 xlsx / 扩展名与内容不符）：
            // Tika 兜底出纯文本（带输出上限），保证调用方拿到可用字符串而非异常穿透到上传链路
            log.warn("primary excel parse failed, fallback to tika: {}", f.getName(), e);
            String fallback = parseWithTikaLimited(f.toPath());
            if (fallback.startsWith("[文档解析失败]")) {
                throw e;   // Tika 也救不回来：还原原始异常，让上层走既有错误处理
            }
            return fallback;
        }
    }

    /**
     * 文件头魔数嗅探（filename 缺失时的分流依据）。
     * 前 8 字节既用于魔数比对，也一并参与后续可打印校验（不漏检前 8 字节）。
     */
    private PathKind sniffPathKind(File f) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
            byte[] head = in.readNBytes(8);
            if (matches(head, OLE2_MAGIC) || matches(head, ZIP_MAGIC)) {
                return PathKind.EXCEL;
            }
            // 非 OLE2/ZIP：读前 512 字节（连同 head 一起）判断是否为文本特征
            byte[] sample = new byte[512];
            int n = in.readNBytes(sample, 0, sample.length);
            // head + sample 拼接校验：前 8 字节不再漏检
            byte[] all = new byte[head.length + n];
            System.arraycopy(head, 0, all, 0, head.length);
            System.arraycopy(sample, 0, all, head.length, n);
            return looksLikeText(all) ? PathKind.CSV : PathKind.EXCEL;
        } catch (Exception e) {
            return PathKind.EXCEL;   // 嗅探失败保守按二进制 Excel 处理（POI 会给出明确报错）
        }
    }

    /** 文本特征判断：ASCII 可打印/常见控制字符，或含 UTF-8 多字节（中文 csv 常见，保守判文本） */
    private static boolean looksLikeText(byte[] bytes) {
        for (byte b : bytes) {
            boolean printable = (b >= 0x20 && b <= 0x7E) || b == '\r' || b == '\n' || b == '\t';
            boolean utf8Multi = (b & 0x80) != 0;
            if (!printable && !utf8Multi) {
                return false;
            }
        }
        return true;
    }

    private enum PathKind { EXCEL, CSV }

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

    /** POI 全量 + Tika 兜底的组合入口（filename 缺失嗅探分流后也走这里） */
    private String parseNormalWithFallback(File f) throws Exception {
        try {
            return parseNormal(f);
        } catch (Exception e) {
            log.warn("poi parse failed, fallback to tika: {}", f.getName(), e);
            String fallback = parseWithTikaLimited(f.toPath());
            if (fallback.startsWith("[文档解析失败]")) {
                throw e;
            }
            return fallback;
        }
    }

    // ==================================================================
    // POI 全量解析（小文件 / xls / xlsm）
    // ==================================================================

    private String parseNormal(File f) throws Exception {
        try (Workbook wb = WorkbookFactory.create(f)) {
            DataFormatter fmt = new DataFormatter();
            // 公式求值器：复用同一实例（创建成本高）；缓存失效/未保存的公式也可求出结果
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            StringBuilder out = new StringBuilder();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                // 隐藏/深度隐藏 sheet 通常是辅助数据，跳过
                if (wb.isSheetHidden(i) || wb.isSheetVeryHidden(i)) {
                    continue;
                }
                Sheet sheet = wb.getSheetAt(i);
                fillMergedRegions(sheet, fmt, evaluator);   // 合并单元格：先填满区域值再输出
                int cols = sheetColumnCount(sheet);
                if (cols == 0) {
                    continue;
                }
                out.append("## ").append(wb.getSheetName(i)).append("\n\n");

                // 用"是否已写表头"标记，不用 rowNum==0：
                // "前几行说明文字、表格从第 5 行开始"很常见，首行全空时 rowNum==0 永远不命中
                boolean headerWritten = false;
                int dataRows = 0;
                boolean truncated = false;
                for (Row row : sheet) {
                    String rowMd = rowToMarkdown(row, fmt, evaluator, cols);
                    if (rowMd == null) {
                        continue;                                 // 全空行跳过
                    }
                    if (!headerWritten) {
                        // 第一个非空行当表头
                        out.append(rowMd).append('\n');
                        out.append(separatorRow(cols)).append('\n');
                        headerWritten = true;
                        continue;
                    }
                    if (++dataRows > MAX_ROWS_PER_SHEET) {
                        truncated = true;
                        break;
                    }
                    out.append(rowMd).append('\n');
                }
                // 整个 sheet 全空（连表头都没有）：回退刚写入的标题，不输出空标题
                if (headerWritten) {
                    if (truncated) {
                        out.append("[截断，仅输出前 ").append(MAX_ROWS_PER_SHEET).append(" 行]\n");
                    }
                    out.append('\n');
                } else {
                    int titleLen = ("## " + wb.getSheetName(i) + "\n\n").length();
                    out.setLength(out.length() - titleLen);
                }
            }
            return out.toString().trim();
        }
    }

    // ==================================================================
    // FastExcel 流式解析（大 xlsx）：SAX 逐行读取，不加载全表
    // ==================================================================

    private String parseStreaming(File f) {
        StringBuilder out = new StringBuilder();

        FastExcel.read(f, new AnalysisEventListener<Map<Integer, String>>() {
            /** 当前正在累积输出的 sheet 名，作为切分哨兵 */
            private String currentSheet = null;
            /** 表头是否待定：表头行全空的 sheet 延迟到首个非空数据行再写表头/定列数 */
            private boolean headerPending = false;
            private final StringBuilder sheetMd = new StringBuilder();
            private int cols = 0;
            private int rows = 0;
            private boolean truncated = false;

            /** 开始一个新 sheet：flush 上一个，写标题；表头非空立即写表头，否则延迟 */
            private void beginSheet(String sheetName, Map<Integer, String> header) {
                flush();
                currentSheet = sheetName;
                rows = 0;
                truncated = false;
                sheetMd.setLength(0);
                sheetMd.append("## ").append(sheetName).append("\n\n");
                if (header.isEmpty()) {
                    // 哨兵路径（表头行全空 / invokeHeadMap 未回调）：
                    // 列数未知，延迟到首个非空数据行定列数——
                    // 否则 cols=1 会导致该 sheet 所有数据列被截断
                    cols = 0;
                    headerPending = true;
                    return;
                }
                writeHeader(header);
            }

            /** 写表头 + 分隔行：列数 = 表头最大物理列索引 + 1。
             *  headMap 的 key 是物理列索引且可能稀疏（中间空列不出现在 map 中），
             *  用 size() 会算错列数导致数据列错位/截断 */
            private void writeHeader(Map<Integer, String> header) {
                int maxKey = header.keySet().stream().max(Integer::compareTo).orElse(-1);
                cols = Math.max(maxKey + 1, 1);
                headerPending = false;
                sheetMd.append(mapRowToMarkdown(header, cols)).append('\n');
                sheetMd.append(separatorRow(cols)).append('\n');
            }

            /** 把已累积的 sheet 内容提交到总输出 */
            private void flush() {
                if (currentSheet == null) {
                    return;
                }
                if (truncated) {
                    sheetMd.append("[截断，仅输出前 ").append(MAX_ROWS_PER_SHEET).append(" 行]\n");
                }
                out.append(sheetMd).append('\n');
                currentSheet = null;
                headerPending = false;
            }

            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                beginSheet(context.readSheetHolder().getSheetName(), headMap);
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // 表头行全空时 invokeHeadMap 不会被回调：用 sheet 名哨兵检测进入新 sheet，
                // 补一个空表头占位（真正的表头延迟到首个非空数据行），防止数据丢失或拼进上一个 sheet
                String sheetName = context.readSheetHolder().getSheetName();
                if (!sheetName.equals(currentSheet)) {
                    beginSheet(sheetName, Collections.emptyMap());
                }
                // 空数据行跳过：不占 MAX_ROWS_PER_SHEET 限额，也不产出噪音行，
                // 也不应被误当表头
                if (isBlankRow(data)) {
                    return;
                }
                if (headerPending) {
                    // 首个非空数据行当表头并定列数（与 POI 路径 headerWritten 语义一致）：
                    // "首行是说明文字、表头在第 2 行"的大表在此正确输出全部列
                    writeHeader(data);
                    return;
                }
                if (++rows > MAX_ROWS_PER_SHEET) {
                    truncated = true;
                    return;
                }
                // 数据列宽于表头时截断到 cols（流式路径无法回改已输出的表头行；
                // 表头宽于数据时 mapRowToMarkdown 自动补空列）
                sheetMd.append(mapRowToMarkdown(data, cols)).append('\n');
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                flush();
            }
        }).doReadAll();

        return out.toString().trim();
    }

    /** FastExcel Map 行是否全为空 */
    private static boolean isBlankRow(Map<Integer, String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String v : row.values()) {
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }

    // ==================================================================
    // CSV 流式解析
    // ==================================================================

    private String parseCsv(File f) throws Exception {
        Charset cs = detectCsvCharset(f);
        StringBuilder md = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(f.toPath(), cs)) {
            stripBom(reader);

            List<String> header = nextRecord(reader);
            if (header == null || isBlankRecord(header)) {
                return "";
            }
            int cols = header.size();
            md.append("## ").append(f.getName()).append("\n\n");
            md.append(listRowToMarkdown(header, cols)).append('\n');
            md.append(separatorRow(cols)).append('\n');

            int rows = 0;
            List<String> record;
            while ((record = nextRecord(reader)) != null) {
                if (isBlankRecord(record)) {
                    continue;                                     // 跳过空行
                }
                if (++rows > MAX_ROWS_PER_SHEET) {
                    md.append("[截断，仅输出前 ").append(MAX_ROWS_PER_SHEET).append(" 行]\n");
                    break;
                }
                md.append(listRowToMarkdown(record, cols)).append('\n');
            }
        }
        return md.toString().trim();
    }

    /** 剥离 UTF-8 BOM（\uFEFF）：Excel 导出的 UTF-8 CSV 常带，不剥则表头首列永远检索不到。
     *  UTF-16 文件由 Files.newBufferedReader 按 UTF_16 解码（自带 BOM 处理），不走此方法。 */
    private void stripBom(BufferedReader reader) throws IOException {
        reader.mark(1);
        int c = reader.read();
        if (c != 0xFEFF) {
            reader.reset();
        }
    }

    /**
     * CSV 编码嗅探：
     * 1. UTF-8 BOM → UTF-8；UTF-16LE/BE BOM → UTF_16（由 JDK 按声明解码）；
     * 2. 无 BOM：对前 8KB 做严格 UTF-8 解码校验，通过则 UTF-8；
     * 3. 校验失败 → GB18030（GBK 超集，覆盖国内 Excel 导出的 CSV）。
     * 纯 ASCII 两种编码结果一致，归入 UTF-8。
     * <p>
     * 注意：8KB 采样可能恰好切断 UTF-8 多字节字符的尾部 → 校验误报 malformed →
     * UTF-8 文件被误判 GB18030 → 解析失败。因此校验前裁掉尾部不完整序列。
     */
    private Charset detectCsvCharset(File f) throws IOException {
        byte[] head;
        try (InputStream in = new FileInputStream(f)) {
            head = in.readNBytes(8192);
        }
        // UTF-8 BOM
        if (head.length >= 3 && (head[0] & 0xFF) == 0xEF
            && (head[1] & 0xFF) == 0xBB && (head[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        // UTF-16 BOM（FF FE = LE / FE FF = BE）：JDK 的 UTF_16 charset 按声明自动处理
        if (head.length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16;
        }
        if (head.length >= 2 && (head[0] & 0xFF) == 0xFE && (head[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16;
        }
        try {
            CharsetDecoder dec = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
            // 裁掉 8KB 边界切断的多字节尾部：去掉尾部 UTF-8 续字节 10xxxxxx，
            // 若剩余尾部是多字节前导字节（110/1110/11110 开头）也一并去掉
            int end = head.length;
            while (end > 0 && (head[end - 1] & 0xC0) == 0x80) {
                end--;
            }
            if (end > 0 && (head[end - 1] & 0x80) != 0) {
                end--;
            }
            dec.decode(ByteBuffer.wrap(head, 0, end));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return GB18030;
        }
    }

    /**
     * 读取一条完整 CSV 记录（RFC4180 状态机）：
     * - 逐字符解析，行尾仍处于引号内时续读下一行（引号内换行正确并入字段）；
     * - "" 转义为 "；
     * - 引号内字段保留原始内容不 trim，非引号字段 trim（兼容脏数据的前后空格）；
     * - 字段内裸引号（如 ab"cd、3"管）：宽容处理为普通字符保留，
     *   不进入引号模式（旧实现在此无条件开启 inQuotes，导致逗号失效 + 错误续行吞掉后续行）。
     * 文件读完返回 null。
     */
    private List<String> nextRecord(BufferedReader reader) throws IOException {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        boolean quotedField = false;
        boolean sawAnyLine = false;

        String line;
        while ((line = reader.readLine()) != null) {
            sawAnyLine = true;
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (inQuotes) {
                    if (ch == '"') {
                        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                            cur.append('"');                      // 双引号转义
                            i++;
                        } else {
                            inQuotes = false;
                        }
                    } else {
                        cur.append(ch);
                    }
                } else if (ch == '"') {
                    if (cur.isEmpty()) {
                        inQuotes = true;                          // 只有引号开头的字段才进入引号模式
                        quotedField = true;
                    } else {
                        cur.append('"');                          // 裸引号宽容处理：保留原字符
                    }
                } else if (ch == ',') {
                    addCsvCell(cells, cur, quotedField);
                    cur.setLength(0);
                    quotedField = false;
                } else {
                    cur.append(ch);
                }
            }
            if (!inQuotes) {
                break;                                            // 记录在本行内闭合，结束
            }
            cur.append('\n');                                     // 引号内换行，续读下一行
        }
        if (!sawAnyLine) {
            return null;
        }
        addCsvCell(cells, cur, quotedField);
        return cells;
    }

    private void addCsvCell(List<String> cells, StringBuilder cur, boolean quoted) {
        String v = cur.toString();
        if (!quoted) {
            v = v.trim();
        }
        cells.add(v);
    }

    /** 判断一条记录是否全为空字段（空行） */
    private boolean isBlankRecord(List<String> record) {
        for (String c : record) {
            if (c != null && !c.isBlank()) {
                return false;
            }
        }
        return true;
    }

    // ==================================================================
    // 合并单元格 / 行列转换
    // ==================================================================

    /**
     * 合并单元格填值：将区域左上角值填充到区域内所有单元格。
     * 区域内除左上角外的格子物理上可能不存在（合并时被删除），
     * 必须用 createCell 补齐后写值，否则输出仍是空值，"填满区域"的目的落空。
     * 左上角为空值时跳过填值：避免批量 createCell("") 虚增列数、产出多余空列。
     * （调用顺序在本方法之后才算 sheetColumnCount，补 cell 不影响列数统计。）
     */
    private void fillMergedRegions(Sheet sheet, DataFormatter fmt, FormulaEvaluator evaluator) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            Row headRow = sheet.getRow(region.getFirstRow());
            if (headRow == null) {
                continue;
            }
            Cell headCell = headRow.getCell(region.getFirstColumn());
            String value = headCell == null ? "" : cellText(headCell, fmt, evaluator);
            if (value == null || value.isEmpty()) {
                continue;   // 左上角为空：跳过（不批量创建空 cell）
            }
            for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                    if (r == region.getFirstRow() && c == region.getFirstColumn()) {
                        continue;
                    }
                    Cell cell = row.getCell(c);
                    if (cell == null) {
                        cell = row.createCell(c);                 // 补齐不存在的格子
                    }
                    cell.setCellValue(value);
                }
            }
        }
    }

    /** 计算 sheet 最大列数（取所有行的最大 lastCellNum） */
    private int sheetColumnCount(Sheet sheet) {
        int max = 0;
        for (Row row : sheet) {
            int last = row.getLastCellNum();
            if (last > max) {
                max = last;
            }
        }
        return max;
    }

    /**
     * POI 行转 Markdown 表格行。
     * 全空行返回 null（由调用方跳过，避免产出 "| | |" 噪音行）。
     */
    private String rowToMarkdown(Row row, DataFormatter fmt, FormulaEvaluator evaluator, int cols) {
        boolean anyContent = false;
        StringBuilder sb = new StringBuilder("| ");
        for (int c = 0; c < cols; c++) {
            Cell cell = row.getCell(c);
            String text = cell == null ? "" : cellText(cell, fmt, evaluator);
            if (!text.isBlank()) {
                anyContent = true;
            }
            sb.append(escapeCell(text)).append(" | ");
        }
        return anyContent ? sb.toString() : null;
    }

    /** FastExcel Map 行转 Markdown 表格行（缺失列/表头宽于数据时自动补空列） */
    private String mapRowToMarkdown(Map<Integer, String> row, int cols) {
        boolean anyContent = false;
        StringBuilder sb = new StringBuilder("| ");
        for (int c = 0; c < cols; c++) {
            String v = row.get(c);
            if (v != null && !v.isBlank()) {
                anyContent = true;
            }
            sb.append(escapeCell(v == null ? "" : v)).append(" | ");
        }
        return anyContent ? sb.toString() : "| " + " | ".repeat(Math.max(cols - 1, 0)) + " |";
    }

    /** CSV List 行转 Markdown 表格行：按表头列数补齐/截断，脏行（多/少一列）不破坏表格结构 */
    private String listRowToMarkdown(List<String> cells, int cols) {
        boolean anyContent = false;
        StringBuilder sb = new StringBuilder("| ");
        for (int i = 0; i < cols; i++) {
            String v = i < cells.size() ? cells.get(i) : null;
            if (v != null && !v.isBlank()) {
                anyContent = true;
            }
            sb.append(escapeCell(v == null ? "" : v)).append(" | ");
        }
        return anyContent ? sb.toString() : null;
    }

    /** Markdown 表头分隔行 */
    private String separatorRow(int cols) {
        return "| " + String.join(" | ", Collections.nCopies(cols, "---")) + " |";
    }

    /** 单元格文本清洗：压缩空白、超长截断、转义竖线（Markdown 表格分隔符）。
     *  截断时避开 emoji 等增补字符的代理对中间（按 char 截可能切出非法字符） */
    private String escapeCell(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replaceAll("\\s+", " ").trim();
        if (t.length() > MAX_CELL_CHARS) {
            int end = MAX_CELL_CHARS;
            // 截断点落在代理对的高位（高代理 \uD800-\uDBFF）时回退一位，保持代码点完整
            if (Character.isHighSurrogate(t.charAt(end - 1))) {
                end--;
            }
            t = t.substring(0, end) + "...";
        }
        return t.replace("|", "\\|");
    }

    /**
     * 按显示格式取单元格值：
     * - 普通单元格：DataFormatter 处理日期/数字/文本格式；
     * - 公式单元格：formatCellValue(cell, evaluator) 主动求值——
     *   公式缓存可能失效（外部引用/未保存），只读缓存会得到 0 或空；
     * - 求值失败（如 POI 不支持的函数）回退为缓存值，再失败返回空串。
     */
    private String cellText(Cell cell, DataFormatter fmt, FormulaEvaluator evaluator) {
        try {
            String v;
            if (cell.getCellType() == CellType.FORMULA && evaluator != null) {
                try {
                    v = fmt.formatCellValue(cell, evaluator);
                } catch (Exception e) {
                    v = fmt.formatCellValue(cell);                // 求值失败回退缓存值
                }
            } else {
                v = fmt.formatCellValue(cell);
            }
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }
}
