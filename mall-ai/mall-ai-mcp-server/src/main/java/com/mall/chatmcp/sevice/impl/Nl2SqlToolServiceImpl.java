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
            "(?i)(union\\s+select|insert\\s+into|delete\\s+from|update\\s+\\w+\\s+set|drop\\s+table|truncate\\s+table|exec\\s+|execute\\s+|xp_cmdshell|sp_\\w+|--|;\\s*select|\\|\\|\\s*select|\\|\\s*select)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SQL_PATTERN = Pattern.compile(
            "(?s)(```sql\\s*)?(SELECT|select)(.*?)(```)?",
            Pattern.DOTALL
    );

    @Tool(description = "自然语言转SQL查询工具。用户输入自然语言问题，系统自动生成并执行SQL语句返回结果。 ")
    public AjaxResult nl2SqlQuery(SqlQueryBo sqlQueryBo) {
        BindingResult bindingResult = new BeanPropertyBindingResult(sqlQueryBo, "sqlQueryBo");
        validator.validate(sqlQueryBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("参数校验失败：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }

        try {
            Map<String, Object> result = new HashMap<>();

            String schemaInfo = extractSchema(sqlQueryBo.getTableNames());
            result.put("schemaInfo", schemaInfo);

            String generatedSql = generateSql(sqlQueryBo.getQuestion(), schemaInfo);
            if (generatedSql == null || generatedSql.isEmpty()) {
                return AjaxResult.error("无法生成有效的SQL语句，请检查您的问题描述");
            }
            result.put("generatedSql", generatedSql);

            if (!validateSql(generatedSql)) {
                return AjaxResult.error("生成的SQL语句包含不安全内容，已拒绝执行");
            }

            List<Map<String, Object>> queryResult = executeSql(generatedSql);
            result.put("result", queryResult);
            result.put("rowCount", queryResult.size());

            String summary = summarizeResult(sqlQueryBo.getQuestion(), queryResult);
            result.put("summary", summary);

            return AjaxResult.success("查询成功", result);
        } catch (Exception e) {
            return AjaxResult.error("查询失败：" + e.getMessage());
        }
    }

    private String extractSchema(String[] tableNames) {
        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("以下是数据库的表结构信息：\n\n");

        List<String> tables;
        if (tableNames != null && tableNames.length > 0) {
            tables = Arrays.asList(tableNames);
        } else {
            tables = getAllTableNames();
        }

        for (String tableName : tables) {
            schemaBuilder.append("表名：").append(tableName).append("\n");
            schemaBuilder.append("字段列表：\n");

            try {
                List<Map<String, Object>> columns = getTableColumns(tableName);
                for (Map<String, Object> column : columns) {
                    String columnName = column.get("COLUMN_NAME").toString();
                    String dataType = column.get("DATA_TYPE").toString();
                    String columnComment = column.getOrDefault("COLUMN_COMMENT", "").toString();
                    boolean isNullable = "YES".equals(column.get("IS_NULLABLE"));

                    schemaBuilder.append("  - ").append(columnName)
                            .append(" (").append(dataType)
                            .append(isNullable ? ", 可空" : ", 非空")
                            .append(columnComment.isEmpty() ? "" : ", 备注: ").append(columnComment)
                            .append(")\n");
                }
                schemaBuilder.append("\n");
            } catch (Exception e) {
                schemaBuilder.append("  获取表结构失败：").append(e.getMessage()).append("\n\n");
            }
        }

        return schemaBuilder.toString();
    }

    private List<String> getAllTableNames() {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    private List<Map<String, Object>> getTableColumns(String tableName) {
        String sql = "SELECT column_name, data_type, is_nullable, column_comment FROM information_schema.columns WHERE table_name = ?";
        return jdbcTemplate.query(sql, new Object[]{tableName}, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("COLUMN_NAME", rs.getString("column_name"));
                map.put("DATA_TYPE", rs.getString("data_type"));
                map.put("IS_NULLABLE", rs.getString("is_nullable"));
                map.put("COLUMN_COMMENT", rs.getString("column_comment"));
                return map;
            }
        });
    }

    private String generateSql(String question, String schemaInfo) {
//        if (chatClient == null) {
//            return null;
//        }

        String systemPrompt = """
                你是一个专业的SQL生成专家。请根据用户的自然语言问题和数据库表结构，生成正确的MySQL SQL查询语句。

                严格遵循以下规则：
                1. 只生成SELECT查询语句，禁止生成INSERT、UPDATE、DELETE、DROP、TRUNCATE等任何修改数据的语句
                2. 使用正确的表名和字段名，参考提供的表结构信息
                3. 如果需要聚合函数，使用COUNT、SUM、AVG、MAX、MIN等标准SQL函数
                4. 如果需要排序，使用ORDER BY子句
                5. 如果需要限制返回条数，使用LIMIT子句，默认不超过100条
                6. 如果需要日期范围，使用DATE_SUB、NOW()等MySQL日期函数
                7. SQL语句必须是完整的、可执行的
                8. 只返回SQL语句，不要包含其他解释性文字或markdown代码块标记
                9. 如果无法确定查询逻辑，返回空字符串

                数据库表结构：
                {schemaInfo}

                请将以下自然语言问题转换为SQL：
                """;

//        SystemPromptTemplate systemTemplate = new SystemPromptTemplate(systemPrompt);
//        UserPromptTemplate userTemplate = new UserPromptTemplate("{question}");
//
//        Prompt prompt = new Prompt(
//                systemTemplate.createMessage(Map.of("schemaInfo", schemaInfo)),
//                userTemplate.createMessage(Map.of("question", question))
//        );
//
//        var response = chatClient.prompt(prompt).call();
        String content = "";//response.chatResponse().getResult().getOutput().getContent();

        return extractSqlFromContent(content);
    }

    private String extractSqlFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        Matcher matcher = SQL_PATTERN.matcher(content);
        if (matcher.find()) {
            return "SELECT" + matcher.group(2).trim();
        }

        return content.trim();
    }

    private boolean validateSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }

        Matcher injectionMatcher = SQL_INJECTION_PATTERN.matcher(sql);
        if (injectionMatcher.find()) {
            return false;
        }

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
            return "未查询到相关数据";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("查询结果如下：\n\n");

        if (result.size() <= 10) {
            Map<String, Object> firstRow = result.get(0);
            summary.append("表头：").append(String.join(", ", firstRow.keySet())).append("\n");
            summary.append("数据：\n");

            for (int i = 0; i < result.size(); i++) {
                Map<String, Object> row = result.get(i);
                summary.append("  ").append(i + 1).append(". ");
                List<String> values = new ArrayList<>();
                for (Object value : row.values()) {
                    values.add(value != null ? value.toString() : "null");
                }
                summary.append(String.join(", ", values)).append("\n");
            }
        } else {
            summary.append("共查询到 ").append(result.size()).append(" 条数据\n");
            summary.append("前5条数据：\n");

            Map<String, Object> firstRow = result.get(0);
            summary.append("表头：").append(String.join(", ", firstRow.keySet())).append("\n");

            for (int i = 0; i < Math.min(5, result.size()); i++) {
                Map<String, Object> row = result.get(i);
                summary.append("  ").append(i + 1).append(". ");
                List<String> values = new ArrayList<>();
                for (Object value : row.values()) {
                    values.add(value != null ? value.toString() : "null");
                }
                summary.append(String.join(", ", values)).append("\n");
            }
            summary.append("...（还有 ").append(result.size() - 5).append(" 条数据）");
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

    @Tool(description = "获取指定表的详细结构信息，包括字段名、数据类型、是否可空、备注等。 ")
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