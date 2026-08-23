package com.mall.chatmcp.sevice.impl;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteKbRagRetrieveService;
import com.mall.system.api.RemoteSqlService;
import com.mall.system.api.domain.SqlQueryRequest;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * 自然语言转SQL查询工具
 * <p>
 * 完整执行流程：
 * 1. 用户消息传入 → Feign调用chat服务检索 kbType=20 的知识库
 * 2. chat服务端将用户消息与知识库内容的标签（tags）进行反向匹配，找出所有相似的 KbDocument
 * 3. 从匹配的 KbDocument 获取 getKnowledgeId，携带 tags + knowledgeId 双重过滤去向量库查询
 * 4. 对向量检索结果进行 Reranker 重排序，返回最相关的表结构Schema
 * 5. 基于Schema生成SQL → 安全校验（白名单表名 + JSqlParser AST）→ 强制LIMIT → 执行 → 返回结果
 */
@Service
public class Nl2SqlToolServiceImpl extends BaseToolServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(Nl2SqlToolServiceImpl.class);

    /** 知识库类型：20 = NL2SQL表结构专业知识 */
    private static final String KB_TYPE_NL2SQL = "20";

    /** 查询最大返回行数（安全防护） */
    private static final long MAX_ROWS = 100L;

    @Autowired
    private RemoteSqlService remoteSqlService;

    @Autowired
    @Lazy
    @Qualifier("sqlChatClient")
    private ChatClient sqlChatClient;

    @Autowired
    private RemoteKbRagRetrieveService remoteKbRagRetrieveService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = """
        【必须使用的查询工具】任何需要获取数据、统计数量、查询列表的问题，
        无论检索结果如何，都必须优先调用本工具。
        输入自然语言问题，工具会自动生成并执行 SQL。
        参数：question(必填, 自然语言问题, 例如: 统计每个部门的人数)
    """)
    public AjaxResult nl2SqlQuery(@ToolParam(description = "查询问题，如：查询所有用户信息") String question) {
        return executeWithErrorHandling(() -> {
            // ========== 步骤1&2&3&4：检索知识库（Feign调用 kbType=20） ==========
            // chat服务端内部流程：
            //   a) 查询 kbType=20 的知识库
            //   b) 将用户消息与知识库内容的 tags 反向匹配，找出所有相似的 KbDocument
            //   c) 获取 KbDocument.getKnowledgeId()，用 tags + knowledgeId 双重过滤向量库
            //   d) Reranker 重排序后返回最相关的表结构Schema
            logger.info("[SQL工具] 从知识库检索表结构（kbType={}）...", KB_TYPE_NL2SQL);
            String effectiveSchema = retrieveSchema(question);

            // ========== 步骤5：生成SQL → 校验 → 执行（含自我修正重试） ==========
            SqlExecutionResult executionResult = generateValidateAndExecute(question, effectiveSchema);

            // 封装返回结果
            return wrapResult(executionResult);
        }, "自然语言转SQL查询工具");
    }

    /**
     * 步骤1-4：从知识库检索表结构Schema
     * <p>
     * Feign调用chat服务的RAG检索接口，chat服务端内部完成：
     * 1. 查询 kbType=20 的知识库
     * 2. 用户消息与知识库标签（tags）反向匹配，找出所有相似的 KbDocument
     * 3. 获取 getKnowledgeId → 携带 tags + knowledgeId 双重过滤去向量库查询
     * 4. Reranker 重排序 → 返回最相关的Schema片段
     */
    private String retrieveSchema(String question) {
        R<String> ragResult = remoteKbRagRetrieveService.retrieve(question, KB_TYPE_NL2SQL);
        if (ragResult.getCode() != 200 || ragResult.getData() == null || ragResult.getData().isEmpty()) {
            throw new RuntimeException("未在知识库中检索到相关表结构，请在知识库中补充表结构信息");
        }
        String schema = ragResult.getData();
        logger.info("[SQL工具] 知识库检索Schema完成，长度: {} 字符", schema.length());
        return schema;
    }

    /**
     * 生成SQL → 安全校验 → 执行，支持自我修正重试
     * <p>
     * 重试触发条件（最多重试1次）：
     * - 表名白名单校验失败（LLM幻觉出不存在的表）
     * - SQL执行失败（列名错误、语法错误等，将错误信息反馈给LLM修正）
     */
    private SqlExecutionResult generateValidateAndExecute(String question, String schema) {
        String lastError = "";
        int maxRetries = 1;

        // 从Schema中提取合法表名白名单（用于防止LLM幻觉表名）
        Set<String> allowedTables = extractTableNames(schema);

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            // 1. 生成SQL（带上上次的错误信息进行自我修正）
            String generatedSql = generateSql(question, schema, lastError);
            if (generatedSql.isEmpty()) {
                throw new RuntimeException("AI无法生成有效的SQL语句，请尝试更清晰的描述");
            }

            // 2. JSqlParser 安全检查（拒绝非SELECT语句和注入）
            if (!validateSql(generatedSql)) {
                throw new RuntimeException("生成的SQL语句包含不安全内容或非查询语句，已拒绝执行");
            }

            // 3. 表名白名单校验（防止LLM幻觉出Schema中不存在的表）
            List<String> missingTables = findMissingTables(generatedSql, allowedTables);
            if (!missingTables.isEmpty()) {
                lastError = "SQL使用了知识库中不存在的表名: " + missingTables + "，请仅使用给定的表结构";
                logger.warn("[SQL工具] 表名白名单校验失败: {}", lastError);
                if (attempt == maxRetries) {
                    throw new RuntimeException(lastError);
                }
                continue;
            }

            // 4. 强制添加 LIMIT（安全防护）
            generatedSql = enforceLimit(generatedSql);

            // 5. 执行SQL（失败则设置lastError进入下一轮修正）
            try {
                List<Map<String, Object>> queryResult = executeSql(generatedSql);
                return new SqlExecutionResult(generatedSql, queryResult);
            } catch (Exception e) {
                lastError = "SQL执行错误: " + e.getMessage();
                logger.warn("[SQL工具] SQL执行失败，尝试自我修正: {}", lastError);
                if (attempt == maxRetries) {
                    throw e;
                }
            }
        }
        throw new RuntimeException("查询失败");
    }

    /**
     * 封装SQL执行结果
     */
    private AjaxResult wrapResult(SqlExecutionResult executionResult) {
        List<Map<String, Object>> queryResult = executionResult.getQueryResult();

        Map<String, Object> result = new HashMap<>();
        result.put("generatedSql", executionResult.getSql());
        result.put("result", queryResult);
        result.put("rowCount", queryResult.size());
        result.put("summary", summarizeResult(queryResult));

        return new AjaxResult(9999, "查询成功", result);
    }

    /**
     * 从Schema文本中提取所有合法表名，构成白名单
     * <p>
     * 识别CREATE TABLE语句中的表名，用于校验生成的SQL不会引用知识库中不存在的表
     */
    private Set<String> extractTableNames(String schema) {
        Set<String> tableNames = new HashSet<>();
        if (schema == null || schema.isEmpty()) {
            return tableNames;
        }
        // 匹配 CREATE TABLE `xxx` 或 CREATE TABLE xxx
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?([a-zA-Z_][a-zA-Z0-9_]*)`?");
        java.util.regex.Matcher matcher = pattern.matcher(schema);
        while (matcher.find()) {
            tableNames.add(matcher.group(1).toLowerCase());
        }
        logger.info("[SQL工具] 从Schema中提取到{}个合法表名: {}", tableNames.size(), tableNames);
        return tableNames;
    }

    /**
     * 检查SQL中引用的表名是否都在白名单内，返回缺失的表名列表
     */
    private List<String> findMissingTables(String sql, Set<String> allowedTables) {
        List<String> missing = new ArrayList<>();
        if (allowedTables == null || allowedTables.isEmpty()) {
            return missing; // 白名单为空时不校验（兼容Schema中无CREATE TABLE的情况）
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select select) {
                List<String> usedTables = new ArrayList<>();
                collectTableNames(select, usedTables);
                for (String table : usedTables) {
                    if (!allowedTables.contains(table.toLowerCase())) {
                        missing.add(table);
                    }
                }
            }
        } catch (JSQLParserException e) {
            logger.warn("[SQL工具] findMissingTables解析失败: {}", e.getMessage());
        }
        return missing;
    }

    /**
     * 递归收集SELECT语句中引用的所有表名（含子查询和JOIN）
     */
    private void collectTableNames(Select select, List<String> tableNames) {
        if (select instanceof PlainSelect plainSelect) {
            // FROM 表
            if (plainSelect.getFromItem() instanceof Table table) {
                tableNames.add(table.getName());
            }
            // JOIN 表
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    if (join.getRightItem() instanceof Table table) {
                        tableNames.add(table.getName());
                    }
                }
            }
            // 子查询（FROM子查询）
            if (plainSelect.getFromItem() instanceof ParenthesedSelect subSelect) {
                collectTableNames(subSelect.getSelect(), tableNames);
            }
        } else if (select instanceof SetOperationList setOp) {
            // UNION/INTERSECT等集合操作
            for (Select sub : setOp.getSelects()) {
                collectTableNames(sub, tableNames);
            }
        } else if (select instanceof ParenthesedSelect parenthesed) {
            collectTableNames(parenthesed.getSelect(), tableNames);
        }
    }

    /**
     * 使用 JSqlParser 安全地强制添加 LIMIT，防止查询过载
     * 相比字符串匹配，AST 解析能准确识别聚合函数和已有 LIMIT，避免误判
     */
    private String enforceLimit(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select)) {
                return sql;
            }

            // 处理普通 SELECT
            if (select instanceof PlainSelect plainSelect) {

                // 1. 聚合查询（COUNT/SUM/AVG/MAX/MIN）不需要 LIMIT
                if (hasAggregateFunction(plainSelect)) {
                    return sql;
                }

                // 2. 已有 LIMIT 则不重复添加
                if (plainSelect.getLimit() != null) {
                    return sql;
                }

                // 3. 安全添加 LIMIT 100
                Limit limit = new Limit();
                limit.setRowCount(new LongValue(MAX_ROWS));
                plainSelect.setLimit(limit);
                return select.toString();
            }

            // 处理 UNION 等集合操作
            if (select instanceof SetOperationList setOp) {
                if (setOp.getLimit() != null) {
                    return sql;
                }
                Limit limit = new Limit();
                limit.setRowCount(new LongValue(MAX_ROWS));
                setOp.setLimit(limit);
                return select.toString();
            }

            return sql;
        } catch (JSQLParserException e) {
            logger.warn("[SQL工具] enforceLimit 解析失败，返回原始SQL: {}", e.getMessage());
            return sql;
        }
    }

    /**
     * 检查 SELECT 是否包含聚合函数（COUNT/SUM/AVG/MAX/MIN）
     */
    private boolean hasAggregateFunction(PlainSelect plainSelect) {
        if (plainSelect.getSelectItems() == null) {
            return false;
        }
        for (SelectItem<?> item : plainSelect.getSelectItems()) {
            if (item.getExpression() instanceof Function func) {
                String funcName = func.getName().toUpperCase();
                if (funcName.equals("COUNT") || funcName.equals("SUM") ||
                    funcName.equals("AVG") || funcName.equals("MAX") || funcName.equals("MIN")) {
                    return true;
                }
            }
        }
        return false;
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
        promptBuilder.append("A: SELECT * FROM sys_user LIMIT 100;\n");
        promptBuilder.append("Q: 查询姓名为'张'的用户\n");
        promptBuilder.append("A: SELECT * FROM sys_user WHERE user_name LIKE '%张%' OR nick_name LIKE '%张%' LIMIT 100;\n");
        promptBuilder.append("Q: 查询部门名称为'研发'的部门\n");
        promptBuilder.append("A: SELECT * FROM sys_dept WHERE dept_name LIKE '%研发%' LIMIT 100;\n");
        promptBuilder.append("Q: 查询岗位名称为'经理'的岗位\n");
        promptBuilder.append("A: SELECT * FROM sys_post WHERE post_name LIKE '%经理%' OR post_code LIKE '%经理%' LIMIT 100;\n");
        promptBuilder.append("Q: 查询角色名称为'管理员'的角色\n");
        promptBuilder.append("A: SELECT * FROM sys_role WHERE role_name LIKE '%管理员%' OR role_key LIKE '%管理员%' LIMIT 100;\n\n");

        // 4. 用户问题
        promptBuilder.append("### 用户问题\n");
        promptBuilder.append(question);
        promptBuilder.append("\n\n### 约束\n");
        promptBuilder.append("1.只能使用上面给定的表结构，禁止使用不存在的表。\n");
        promptBuilder.append("2.只输出SQL语句，不要输出任何解释文字。\n");
        promptBuilder.append("3.当查询条件涉及用户名、部门名称、岗位名称、角色名称等文本字段时，必须使用 LIKE 模糊查询（如：LIKE '%关键词%'），禁止使用 = 精确匹配。\n");
        promptBuilder.append("4.当查询用户名时，必须同时匹配 user_name（用户账号）和 nick_name（用户昵称），使用 OR 连接：user_name LIKE '%关键词%' OR nick_name LIKE '%关键词%'。\n");
        promptBuilder.append("5.当查询部门名称时，使用 dept_name LIKE '%关键词%'。\n");
        promptBuilder.append("6.当查询岗位名称时，必须同时匹配 post_name（岗位名称）和 post_code（岗位编码），使用 OR 连接：post_name LIKE '%关键词%' OR post_code LIKE '%关键词%'。\n");
        promptBuilder.append("7.当查询角色名称时，必须同时匹配 role_name（角色名称）和 role_key（角色权限字符串），使用 OR 连接：role_name LIKE '%关键词%' OR role_key LIKE '%关键词%'。\n");
        promptBuilder.append("8.如果用户输入的是简称（如'研发'），也要用 LIKE 模糊匹配完整名称（如'研发部门'）。\n");

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
    }

    /**
     * 使用 JSqlParser 验证 SQL 安全性
     * 相比正则黑名单，AST 解析能精确识别语句类型，杜绝绕过风险
     */
    private boolean validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        try {
            // JSqlParser 解析 SQL 为 AST
            // 若包含多条语句（分号拼接）或语法非法，会抛出异常
            Statement statement = CCJSqlParserUtil.parse(sql);

            // 必须是 SELECT 语句（自动拒绝 INSERT/UPDATE/DELETE/DROP/TRUNCATE/ALTER/CREATE 等）
            if (!(statement instanceof Select)) {
                logger.warn("[SQL工具] 非SELECT语句被拒绝: {}", statement.getClass().getSimpleName());
                return false;
            }
            return true;
        } catch (JSQLParserException e) {
            logger.warn("[SQL工具] SQL解析失败，可能包含语法错误或注入: {}", e.getMessage());
            return false;
        }
    }

    private List<Map<String, Object>> executeSql(String sql) {
        R<List<Map<String, Object>>> result = remoteSqlService.executeSelect(new SqlQueryRequest(sql));
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

    /**
     * SQL生成+执行结果封装
     */
    private static class SqlExecutionResult {
        private final String sql;
        private final List<Map<String, Object>> queryResult;

        SqlExecutionResult(String sql, List<Map<String, Object>> queryResult) {
            this.sql = sql;
            this.queryResult = queryResult;
        }

        String getSql() {
            return sql;
        }

        List<Map<String, Object>> getQueryResult() {
            return queryResult;
        }
    }

}