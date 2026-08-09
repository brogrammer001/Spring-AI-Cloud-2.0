package com.mall.chatmcp.sevice.impl;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteSqlService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Nl2SqlToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteSqlService remoteSqlService;

    @Autowired
    @Lazy
    @Qualifier("sqlChatClient")
    private ChatClient sqlChatClient;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union\\s+select.*?from|insert\\s+into|delete\\s+from|update\\s+\\w+\\s+set|drop\\s+table|truncate\\s+table|alter\\s+table|create\\s+table|exec\\s+|execute\\s+|xp_cmdshell|sp_\\w+|--|;\\s*select|\\|\\|\\s*select|\\|\\s*select)",
        Pattern.CASE_INSENSITIVE
    );

    @Tool(description = "数据库查询工具。输入自然语言生成并执行SQL。查询问题: question(必填,自然语言问题), tableNames（必填,表名列表）, schemaInfo（必填,表结构文本）。注意参数名必须一致。[JSON]", returnDirect = true)
    public AjaxResult nl2SqlQuery(@ToolParam(description = "查询问题，如：查询所有用户信息") String question,
                                  @ToolParam(description = "表名列表。如：['sys_user']。已传schemaInfo时可留空") String[] tableNames,
                                  @ToolParam(description = "表结构信息。如：Table: sys_user (user_id bigint, user_name varchar);") String schemaInfo) {
        return executeWithErrorHandling(() -> {
            try {
                // 2. 优先使用模型传入的Schema（来自知识库），否则动态查询数据库
                String effectiveSchema;
                if (schemaInfo != null && !schemaInfo.isEmpty()) {
                    effectiveSchema = schemaInfo;
                    logger.info("[SQL工具] 使用知识库传入的Schema，长度: {} 字符", schemaInfo.length());
                } else {
                    logger.info("[SQL工具] 未传入Schema，从数据库动态查询...");
                    effectiveSchema = extractSchema(tableNames);
                    logger.info("[SQL工具] 数据库查询Schema完成，长度: {} 字符", effectiveSchema.length());
                }

                if (effectiveSchema.isEmpty()) {
                    return AjaxResult.error("无法获取数据库表结构信息，请在知识库中补充表结构或指定tableNames参数");
                }

                // 3. 生成SQL（支持自我修正，最多重试1次）
                String generatedSql = "";
                String lastError = "";
                int maxRetries = 1;

                for (int i = 0; i <= maxRetries; i++) {
                    // 生成SQL，带上上次的错误信息（如果有）
                    generatedSql = generateSql(question, effectiveSchema, lastError);

                    if (generatedSql.isEmpty()) {
                        return AjaxResult.error("AI无法生成有效的SQL语句，请尝试更清晰的描述");
                    }

                    if (!validateSql(generatedSql)) {
                        return AjaxResult.error("生成的SQL语句包含不安全内容或非查询语句，已拒绝执行");
                    }

                    // 安全防护：强制检查并添加 LIMIT
                    generatedSql = enforceLimit(generatedSql);

                    try {
                        // 4. 执行SQL
                        List<Map<String, Object>> queryResult = executeSql(generatedSql);

                        // 5. 封装结果
                        Map<String, Object> result = new HashMap<>();
                        result.put("generatedSql", generatedSql);
                        result.put("result", queryResult);
                        result.put("rowCount", queryResult.size());

                        String summary = summarizeResult(queryResult);
                        result.put("summary", summary);

                        return new AjaxResult(9999, "查询成功", result);
                    } catch (Exception e) {
                        // 捕获SQL执行异常
                        lastError = "SQL执行错误: " + e.getMessage();
                        // 如果是最后一次尝试，则抛出异常
                        if (i == maxRetries) {
                            throw e;
                        }
                        // 否则，记录错误，进入下一次循环进行修正
                    }
                }
                return AjaxResult.error("查询失败");
            } catch (Exception e) {
                // 实际项目中建议使用 log.error 记录完整堆栈
                return AjaxResult.error("查询失败：" + e.getMessage());
            }
        }, "自然语言转SQL查询工具");
    }

    /**
     * 强制添加 LIMIT 保护，防止查询过载
     */
    private String enforceLimit(String sql) {
        String upperSql = sql.toUpperCase().trim();
        // 聚合查询通常不需要Limit，或者需要Count全部
        if (upperSql.matches("SELECT\\s+.*?(COUNT|SUM|AVG|MAX|MIN)\\s*\\(")) {
            return sql;
        }
        // 如果已经包含 LIMIT，不再添加
        if (upperSql.contains("LIMIT")) {
            return sql;
        }
        // 强制追加 LIMIT 100
        if (sql.endsWith(";")) {
            return sql.substring(0, sql.length() - 1) + " LIMIT 100;";
        }
        return sql + " LIMIT 100";
    }

    /**
     * 优化后的 Schema 提取，格式更紧凑，利于LLM理解
     */
    private String extractSchema(String[] tableNames) {
        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("### 数据库表结构\n");

        List<String> tables;
        if (tableNames != null && tableNames.length > 0) {
            tables = Arrays.asList(tableNames);
        } else {
            // 如果未指定表，获取所有表（注意：表多时建议限制或要求用户指定）
            tables = getAllTableNames();
        }

        for (String tableName : tables) {
            schemaBuilder.append("\nTable: ").append(tableName).append(" (");
            try {
                List<Map<String, Object>> columns = getTableColumns(tableName);
                List<String> columnDefs = new ArrayList<>();
                for (Map<String, Object> column : columns) {
                    String colName = column.get("COLUMN_NAME").toString();
                    String dataType = column.get("DATA_TYPE").toString();
                    String comment = column.getOrDefault("COLUMN_COMMENT", "").toString();
                    // 格式：字段名 类型 [注释]
                    String def = colName + " " + dataType;
                    if (!comment.isEmpty()) {
                        def += " -- " + comment;
                    }
                    columnDefs.add(def);
                }
                schemaBuilder.append(String.join(", ", columnDefs));
                schemaBuilder.append(");");
            } catch (Exception e) {
                schemaBuilder.append("Error: ").append(e.getMessage()).append(");");
            }
        }
        return schemaBuilder.toString();
    }

    private List<String> getAllTableNames() {
        R<List<String>> result = remoteSqlService.getAllTableNames();
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<Map<String, Object>> getTableColumns(String tableName) {
        R<List<Map<String, Object>>> result = remoteSqlService.getTableColumns(tableName);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    /**
     * 调用LLM生成SQL
     *
     * @param previousError 上一次生成的SQL执行错误信息，用于Self-Correction
     */
    private String generateSql(String question, String schemaInfo, String previousError) {
        StringBuilder promptBuilder = new StringBuilder();

        // 1. 上下文信息
        promptBuilder.append(schemaInfo).append("\n\n");

        // 2. 错误修正指令
        if (previousError != null && !previousError.isEmpty()) {
            promptBuilder.append("### 修正指令\n");
            promptBuilder.append("上一轮生成的SQL执行出错，错误信息如下：\n").append(previousError).append("\n");
            promptBuilder.append("请根据上述错误信息，修正SQL逻辑后重试。不要输出解释，只输出修正后的SQL。\n\n");
        }

        // 3. 示例
        promptBuilder.append("### 示例\n");
        promptBuilder.append("Q: 查询所有用户\n");
        promptBuilder.append("A: SELECT * FROM users LIMIT 100;\n");
        promptBuilder.append("Q: 查询下单金额大于500的用户姓名和订单号\n");
        promptBuilder.append("A: SELECT u.name, o.order_id FROM orders o JOIN users u ON o.user_id = u.id WHERE o.amount > 500 LIMIT 100;\n\n");

        // 4. 用户问题
        promptBuilder.append("### 用户问题\n");
        promptBuilder.append(question);

        // 5. 调用 ChatClient 生成SQL
        String content = sqlChatClient.prompt()
            .user(promptBuilder.toString())
            .call()
            .content();

        return extractSqlFromContent(content);
    }

    /**
     * 修复了正则提取逻辑
     */
    private String extractSqlFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 尝试去除可能存在的 Markdown 代码块标记
        String trimmed = content.trim();
        if (trimmed.startsWith("```sql")) {
            trimmed = trimmed.substring(6);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();

        /*
         * 如果依赖正则提取（备选方案）：
         * Matcher matcher = SQL_PATTERN.matcher(content);
         * if (matcher.find()) {
         *     // Group 2 是 "SELECT" 关键字, Group 3 是剩余内容
         *     return "SELECT " + matcher.group(3).trim();
         * }
         * return content.trim();
         */
    }

    private boolean validateSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }
        // 1. 黑名单检测
        Matcher injectionMatcher = SQL_INJECTION_PATTERN.matcher(sql);
        if (injectionMatcher.find()) {
            return false;
        }
        // 2. 必须以 SELECT 开头
        String upperSql = sql.toUpperCase().trim();
        if (!upperSql.startsWith("SELECT")) {
            return false;
        }
        return true;
    }

    private List<Map<String, Object>> executeSql(String sql) {
        R<List<Map<String, Object>>> result = remoteSqlService.executeSelect(sql);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        throw new RuntimeException(result.getMsg());
    }

    private String summarizeResult(List<Map<String, Object>> result) {
        if (result == null || result.isEmpty()) {
            return "未查询到相关数据。";
        }
        StringBuilder summary = new StringBuilder();
        summary.append("查询结果如下（共 ").append(result.size()).append(" 条）：\n");

        // 仅展示前3条数据作为摘要，避免返回内容过长
        int limit = Math.min(3, result.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> row = result.get(i);
            List<String> values = new ArrayList<>();
            for (Object value : row.values()) {
                values.add(value != null ? value.toString() : "null");
            }
            summary.append(i + 1).append(". ").append(String.join(", ", values)).append("\n");
        }
        if (result.size() > 3) {
            summary.append("...");
        }
        return summary.toString();
    }

}