package com.mall.chatmcp.sevice.impl;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteKbRagRetrieveService;
import com.mall.system.api.RemoteSqlService;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.*;

@Service
public class Nl2SqlToolServiceImpl extends BaseToolServiceImpl {

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

    @Tool(description = "数据库查询工具。输入自然语言生成并执行SQL。参数说明：question(必填,自然语言问题)。")
    public AjaxResult nl2SqlQuery(@ToolParam(description = "查询问题，如：查询所有用户信息") String question) {
        return executeWithErrorHandling(() -> {
            try {
                // Feign调用：从chat服务检索kbType=20表结构专业知识
                logger.info("[SQL工具] 从知识库检索表结构（Feign调用, kbType=20）...");
                R<String> ragResult = remoteKbRagRetrieveService.retrieve(question, "20");
                if (ragResult.getCode() != 200 || ragResult.getData() == null || ragResult.getData().isEmpty()) {
                    return AjaxResult.error("未在知识库中检索到相关表结构，请在知识库中补充表结构信息");
                }
                String effectiveSchema = ragResult.getData();
                logger.info("[SQL工具] 知识库检索Schema完成，长度: {} 字符", effectiveSchema.length());

                // 生成SQL（支持自我修正，最多重试1次）
                String generatedSql;
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
                        // 执行SQL
                        List<Map<String, Object>> queryResult = executeSql(generatedSql);

                        // 封装结果
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
                limit.setRowCount(new LongValue(100));
                plainSelect.setLimit(limit);
                return select.toString();
            }

            // 处理 UNION 等集合操作
            if (select instanceof SetOperationList setOp) {
                if (setOp.getLimit() != null) {
                    return sql;
                }
                Limit limit = new Limit();
                limit.setRowCount(new LongValue(100));
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