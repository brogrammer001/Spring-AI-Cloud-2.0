package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SqlQueryBo;
import com.mall.common.core.web.domain.AjaxResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Nl2SqlToolServiceImpl {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Validator validator;

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union\\s+select.*?from|insert\\s+into|delete\\s+from|update\\s+\\w+\\s+set|drop\\s+table|truncate\\s+table|alter\\s+table|create\\s+table|exec\\s+|execute\\s+|xp_cmdshell|sp_\\w+|--|;\\s*select|\\|\\|\\s*select|\\|\\s*select)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SQL_PATTERN = Pattern.compile(
            "(?s)(```sql\\s*)?(SELECT|select)(.*?)(```)?",
            Pattern.DOTALL
    );

    @Tool(description = "自然语言转SQL查询工具。支持单表查询及多表关联查询。输入自然语言，自动生成并执行SQL。")
    public AjaxResult nl2SqlQuery(SqlQueryBo sqlQueryBo) {
        // 1. 参数校验
        BindingResult bindingResult = new BeanPropertyBindingResult(sqlQueryBo, "sqlQueryBo");
        validator.validate(sqlQueryBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("参数校验失败：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }

        try {
            // 2. 提取Schema（表结构），增加紧凑格式以节省Token
            String schemaInfo = extractSchema(sqlQueryBo.getTableNames());
            if (schemaInfo.isEmpty()) {
                return AjaxResult.error("无法获取数据库表结构信息");
            }

            // 3. 生成SQL（支持自我修正，最多重试1次）
            String generatedSql = "";
            String lastError = "";
            int maxRetries = 1;

            for (int i = 0; i <= maxRetries; i++) {
                // 生成SQL，带上上次的错误信息（如果有）
                generatedSql = generateSql(sqlQueryBo.getQuestion(), schemaInfo, lastError);

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

                    String summary = summarizeResult(sqlQueryBo.getQuestion(), queryResult);
                    result.put("summary", summary);

                    return AjaxResult.success("查询成功", result);

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
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    private List<Map<String, Object>> getTableColumns(String tableName) {
        String sql = "SELECT column_name, data_type, column_comment FROM information_schema.columns WHERE table_name = ? AND table_schema = DATABASE()";
        return jdbcTemplate.query(sql, new Object[]{tableName}, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("COLUMN_NAME", rs.getString("column_name"));
                map.put("DATA_TYPE", rs.getString("data_type"));
                map.put("COLUMN_COMMENT", rs.getString("column_comment"));
                return map;
            }
        });
    }

    /**
     * 调用LLM生成SQL
     * @param previousError 上一次生成的SQL执行错误信息，用于Self-Correction
     */
    private String generateSql(String question, String schemaInfo, String previousError) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个精通MySQL的专家。请根据自然语言问题和提供的表结构，生成正确的SQL查询语句。\n\n");

        // 1. 上下文信息
        promptBuilder.append(schemaInfo).append("\n\n");

        // 2. 错误修正指令
        if (previousError != null && !previousError.isEmpty()) {
            promptBuilder.append("### 修正指令\n");
            promptBuilder.append("上一轮生成的SQL执行出错，错误信息如下：\n").append(previousError).append("\n");
            promptBuilder.append("请根据上述错误信息，修正SQL逻辑后重试。不要输出解释，只输出修正后的SQL。\n\n");
        }

        // 3. 规则与示例 (Few-Shot Prompting)
        promptBuilder.append("### 规则与示例\n");
        promptBuilder.append("1. 只生成 SELECT 语句，严禁修改数据。\n");
        promptBuilder.append("2. 多表查询：根据字段名语义（例如 user_id 关联 id）使用 JOIN 或 WHERE 进行关联。\n");
        promptBuilder.append("3. 默认限制：除非用户要求所有数据，否则末尾必须加 LIMIT 100。\n");
        promptBuilder.append("4. 输出格式：直接输出SQL语句，不要包含 markdown 标记（```sql）。\n");
        promptBuilder.append("\n示例：\n");
        promptBuilder.append("Q: 查询所有用户\n");
        promptBuilder.append("A: SELECT * FROM users LIMIT 100;\n");
        promptBuilder.append("Q: 查询下单金额大于500的用户姓名和订单号\n");
        promptBuilder.append("A: SELECT u.name, o.order_id FROM orders o JOIN users u ON o.user_id = u.id WHERE o.amount > 500 LIMIT 100;\n");
        promptBuilder.append("\n");

        // 4. 用户问题
        promptBuilder.append("### 用户问题\n");
        promptBuilder.append(question);

        // 5. 调用 Spring AI (假设已注入 ChatModel)
        // 注意：实际项目中可以使用 PromptTemplate 等更高级的封装
        String content = "";//chatModel.call(promptBuilder.toString());

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
        return jdbcTemplate.query(sql, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new LinkedHashMap<>();
                ResultSetMetaData metaData = rs.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    map.put(columnName, value);
                }
                return map;
            }
        });
    }

    private String summarizeResult(String question, List<Map<String, Object>> result) {
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

    @Tool(description = "获取数据库所有表名列表，用于了解数据库结构。")
    public AjaxResult getAllTables() {
        try {
            List<String> tableNames = getAllTableNames();
            if (tableNames.isEmpty()) {
                return AjaxResult.error("未查询到数据表");
            }
            return AjaxResult.success("查询成功", tableNames);
        } catch (Exception e) {
            return AjaxResult.error("查询失败：" + e.getMessage());
        }
    }

    @Tool(description = "获取指定表的详细结构信息，包括字段名、数据类型、备注等。")
    public AjaxResult getTableStructure(SqlQueryBo sqlQueryBo) {
        if (sqlQueryBo.getTableNames() == null || sqlQueryBo.getTableNames().length == 0) {
            return AjaxResult.error("请指定表名");
        }
        try {
            Map<String, Object> result = new HashMap<>();
            for (String tableName : sqlQueryBo.getTableNames()) {
                List<Map<String, Object>> columns = getTableColumns(tableName);
                result.put(tableName, columns);
            }
            return AjaxResult.success("查询成功", result);
        } catch (Exception e) {
            return AjaxResult.error("查询失败：" + e.getMessage());
        }
    }

}