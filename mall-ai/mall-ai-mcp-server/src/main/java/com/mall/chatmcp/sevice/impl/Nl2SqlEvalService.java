package com.mall.chatmcp.sevice.impl;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteSqlService;
import com.mall.system.api.domain.SqlQueryRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NL2SQL 黄金评测集执行器（运营化度量）
 * <p>
 * 解决"改 Prompt / 调 Reranker / 加证据全凭感觉"的问题：每次迭代跑固定评测集，
 * 输出准确率趋势，让优化有据可依（对齐 Alibaba/析言 GBI 的评测体系）。
 * <ul>
 *   <li>评测集来源：优先读数据库 nl2sql_eval 表（运营可维护），表不存在时降级内置默认集</li>
 *   <li>断言方式：不比对 SQL 文本（写法太多），比对意图类型 + SQL 关键特征 + 最小行数</li>
 *   <li>触发方式：每日 03:00 定时跑（输出日志），或通过 MCP 工具手动触发</li>
 * </ul>
 * 每条用例跑完整链路（召回→生成→校验→执行→语义校验），15 条约需 1~3 分钟，适合低峰期执行。
 */
@Service
public class Nl2SqlEvalService extends BaseToolServiceImpl {

    @Autowired
    private Nl2SqlToolServiceImpl nl2SqlToolService;

    @Autowired
    private RemoteSqlService remoteSqlService;

    /**
     * 评测用例
     *
     * @param question        评测问题
     * @param expectedType    期望意图类型：QUERY / CHAT / CLARIFY
     * @param sqlContains     期望生成的 SQL 包含的片段（大小写不敏感，全部满足才算过）
     * @param sqlNotContains  期望生成的 SQL 不包含的片段
     * @param expectedMinRows 期望最小结果行数（空则不断言）
     */
    public record EvalCase(String question, String expectedType, List<String> sqlContains,
                           List<String> sqlNotContains, Integer expectedMinRows) {
    }

    /** 单条用例执行结果 */
    private record EvalOutcome(boolean pass, String detail) {
    }

    /**
     * 每日 03:00 自动评测（可通过 nl2sql.eval.cron 覆盖，设为 "-" 禁用）
     */
    @Scheduled(cron = "${nl2sql.eval.cron:0 0 3 * * ?}")
    public void scheduledEval() {
        try {
            runEval("每日定时评测");
        } catch (Exception e) {
            logger.error("[NL2SQL评测] 定时评测执行失败: {}", e.getMessage(), e);
        }
    }

    @Tool(description = """
        【NL2SQL评测工具】运行NL2SQL黄金评测集，统计SQL生成的准确率与失败用例明细。
        会逐条执行完整链路（召回+生成+执行+语义校验），全部用例约需1~3分钟，
        适合验证Prompt/检索改动效果或低峰期质量巡检时调用。无参数。
        """)
    public AjaxResult nl2SqlEvalRun() {
        return executeWithErrorHandling(() -> runEval("手动评测"), "NL2SQL评测");
    }

    /**
     * 执行完整评测并输出准确率
     */
    public AjaxResult runEval(String trigger) {
        List<EvalCase> cases = loadCases();
        if (cases.isEmpty()) {
            return AjaxResult.error("评测集为空：nl2sql_eval 表无可启用例且内置默认集为空");
        }

        long start = System.currentTimeMillis();
        int pass = 0;
        List<Map<String, Object>> details = new ArrayList<>(cases.size());

        for (int i = 0; i < cases.size(); i++) {
            EvalCase c = cases.get(i);
            EvalOutcome outcome = evaluate(c);
            if (outcome.pass()) {
                pass++;
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("index", i + 1);
            detail.put("question", c.question());
            detail.put("expectedType", c.expectedType());
            detail.put("pass", outcome.pass());
            detail.put("detail", outcome.detail());
            details.add(detail);
            logger.info("[NL2SQL评测] [{}/{}] {} 「{}」 => {} | {}",
                i + 1, cases.size(), outcome.pass() ? "PASS" : "FAIL", c.question(),
                c.expectedType(), outcome.detail());
        }

        double accuracy = pass * 100.0 / cases.size();
        String accuracyText = String.format("%.1f", accuracy);
        logger.info("[NL2SQL评测] {} 完成：{}/{} 通过，准确率 {}%，耗时 {}ms",
            trigger, pass, cases.size(), accuracyText, System.currentTimeMillis() - start);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("trigger", trigger);
        summary.put("total", cases.size());
        summary.put("passed", pass);
        summary.put("failed", cases.size() - pass);
        summary.put("accuracy", accuracyText + "%");
        summary.put("costMillis", System.currentTimeMillis() - start);
        summary.put("details", details);
        return new AjaxResult(9999, "评测完成，准确率 " + accuracyText + "%", summary);
    }

    /**
     * 执行单条用例并断言：意图类型 → SQL 特征片段 → 最小行数
     */
    private EvalOutcome evaluate(EvalCase c) {
        try {
            AjaxResult result = nl2SqlToolService.nl2SqlQuery(c.question(), null);
            Object codeObj = result.get("code");
            if (!(codeObj instanceof Integer code) || code == 500) {
                return new EvalOutcome(false, "工具执行失败: " + result.get("msg"));
            }
            Object dataObj = result.get("data");
            if (!(dataObj instanceof Map<?, ?> data)) {
                return new EvalOutcome(false, "返回结构异常，缺少data: " + result.get("msg"));
            }

            // 1. 意图类型判定
            String actualType;
            if (Boolean.TRUE.equals(data.get("needClarify"))) {
                actualType = "CLARIFY";
            } else if (Boolean.FALSE.equals(data.get("needQuery"))) {
                actualType = "CHAT";
            } else {
                actualType = "QUERY";
            }
            if (!c.expectedType().equalsIgnoreCase(actualType)) {
                return new EvalOutcome(false, "意图类型不符，期望 " + c.expectedType() + " 实际 " + actualType
                    + "（" + String.valueOf(data.get("clarify") != null ? data.get("clarify") : data.get("reply")) + "）");
            }

            // 2. CHAT/CLARIFY 到此即可判定通过，QUERY 继续断言 SQL 特征
            if (!"QUERY".equals(actualType)) {
                return new EvalOutcome(true, actualType + " 分支符合预期");
            }

            Object sqlObj = data.get("generatedSql");
            String sql = sqlObj == null ? "" : sqlObj.toString().toLowerCase();
            if (sql.isBlank()) {
                return new EvalOutcome(false, "生成的SQL为空");
            }
            for (String expect : c.sqlContains()) {
                if (!sql.contains(expect.toLowerCase())) {
                    return new EvalOutcome(false, "SQL缺少关键特征 [" + expect + "]: " + sql);
                }
            }
            for (String forbid : c.sqlNotContains()) {
                if (sql.contains(forbid.toLowerCase())) {
                    return new EvalOutcome(false, "SQL包含禁止特征 [" + forbid + "]: " + sql);
                }
            }

            // 3. 最小行数断言
            if (c.expectedMinRows() != null) {
                Object rowCountObj = data.get("rowCount");
                int rowCount = rowCountObj instanceof Integer i ? i : 0;
                if (rowCount < c.expectedMinRows()) {
                    return new EvalOutcome(false, "结果行数不足，期望>=" + c.expectedMinRows() + " 实际 " + rowCount);
                }
            }
            return new EvalOutcome(true, "SQL=" + sql);
        } catch (Exception e) {
            return new EvalOutcome(false, "执行异常: " + e.getMessage());
        }
    }

    /**
     * 加载评测集：优先读 nl2sql_eval 表（运营可维护），失败或为空时降级内置默认集
     */
    private List<EvalCase> loadCases() {
        try {
            R<List<Map<String, Object>>> result = remoteSqlService.executeSelect(new SqlQueryRequest(
                "SELECT question, expected_type, expected_sql_contains, expected_sql_not_contains, expected_min_rows "
                    + "FROM nl2sql_eval WHERE enabled = '1' ORDER BY id"));
            if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                List<EvalCase> cases = new ArrayList<>();
                for (Map<String, Object> row : result.getData()) {
                    String type = str(row.get("expected_type"));
                    cases.add(new EvalCase(
                        str(row.get("question")),
                        type.isBlank() ? "QUERY" : type.toUpperCase(),
                        splitCsv(str(row.get("expected_sql_contains"))),
                        splitCsv(str(row.get("expected_sql_not_contains"))),
                        parseInteger(row.get("expected_min_rows"))));
                }
                logger.info("[NL2SQL评测] 从 nl2sql_eval 表加载 {} 条用例", cases.size());
                return cases;
            }
        } catch (Exception e) {
            logger.warn("[NL2SQL评测] 读取评测集表失败（表可能未建），降级使用内置默认集: {}", e.getMessage());
        }
        List<EvalCase> defaults = defaultCases();
        logger.info("[NL2SQL评测] 使用内置默认评测集，共 {} 条用例", defaults.size());
        return defaults;
    }

    /**
     * 内置默认黄金集：覆盖单表/模糊匹配/聚合/JOIN/时间感知/意图分类等核心场景。
     * 与 sql/nl2sql_eval.sql 种子数据保持一致，建表后以表内数据为准。
     */
    private List<EvalCase> defaultCases() {
        List<EvalCase> cases = new ArrayList<>();
        // 单表查询
        cases.add(new EvalCase("查询所有用户信息", "QUERY", List.of("sys_user"), List.of(), null));
        // 模糊匹配
        cases.add(new EvalCase("查询姓名包含'张'的用户", "QUERY", List.of("sys_user", "like", "%张%"), List.of(), null));
        // 聚合统计
        cases.add(new EvalCase("系统里一共有多少个用户", "QUERY", List.of("count"), List.of(), null));
        // 多表 JOIN
        cases.add(new EvalCase("统计每个部门的人数", "QUERY", List.of("sys_dept", "count", "join"), List.of(), null));
        // 状态过滤
        cases.add(new EvalCase("查询所有停用状态的用户", "QUERY", List.of("sys_user", "status"), List.of(), null));
        // 关联部门
        cases.add(new EvalCase("查询研发部门的用户", "QUERY", List.of("sys_user", "dept"), List.of(), null));
        // 角色表
        cases.add(new EvalCase("查询所有角色信息", "QUERY", List.of("sys_role"), List.of(), null));
        // 岗位模糊
        cases.add(new EvalCase("查询岗位名称包含'经理'的岗位", "QUERY", List.of("sys_post", "like"), List.of(), null));
        // 时间感知（相对时间换算）
        cases.add(new EvalCase("上个月新增了多少用户", "QUERY", List.of("sys_user", "create_time"), List.of(), null));
        // 分组统计
        cases.add(new EvalCase("统计男性和女性用户的数量", "QUERY", List.of("sex", "count", "group by"), List.of(), null));
        // 用户角色关联
        cases.add(new EvalCase("查询用户admin拥有的角色", "QUERY", List.of("sys_role"), List.of(), null));
        // 字典表
        cases.add(new EvalCase("查询所有字典类型", "QUERY", List.of("sys_dict_type"), List.of(), null));
        // 意图分类：无需查库
        cases.add(new EvalCase("帮我写一份周报", "CHAT", List.of(), List.of(), null));
        cases.add(new EvalCase("你好，你能做什么", "CHAT", List.of(), List.of(), null));
        // 意图分类：需要澄清
        cases.add(new EvalCase("查一下那个东西的数量", "CLARIFY", List.of(), List.of(), null));
        return cases;
    }

    /** 逗号分隔转列表，空串返回空列表 */
    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split("[,，]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private static Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}
