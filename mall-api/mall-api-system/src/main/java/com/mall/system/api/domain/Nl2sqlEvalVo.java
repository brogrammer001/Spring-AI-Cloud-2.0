package com.mall.system.api.domain;

import java.io.Serializable;

/**
 * NL2SQL黄金评测集对象 nl2sql_eval
 * <p>
 * Feign 远程调用 DTO，与 mall-ai-chat 的 com.mall.aichat.domain.Nl2sqlEval 字段对齐。
 * 仅包含 NL2SQL 执行链路所需的业务字段，省略 BaseEntity 审计字段。
 */
public class Nl2sqlEvalVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用例ID */
    private Long id;

    /** 评测问题 */
    private String question;

    /** 期望意图类型（QUERY/CHAT/CLARIFY） */
    private String expectedType;

    /** 期望SQL包含的片段（逗号分隔，大小写不敏感，全部满足才通过） */
    private String expectedSqlContains;

    /** 期望SQL不包含的片段（逗号分隔） */
    private String expectedSqlNotContains;

    /** 期望最小结果行数（NULL则不断言行数） */
    private Long expectedMinRows;

    /** 是否启用（1是 0否） */
    private String enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(String expectedType) {
        this.expectedType = expectedType;
    }

    public String getExpectedSqlContains() {
        return expectedSqlContains;
    }

    public void setExpectedSqlContains(String expectedSqlContains) {
        this.expectedSqlContains = expectedSqlContains;
    }

    public String getExpectedSqlNotContains() {
        return expectedSqlNotContains;
    }

    public void setExpectedSqlNotContains(String expectedSqlNotContains) {
        this.expectedSqlNotContains = expectedSqlNotContains;
    }

    public Long getExpectedMinRows() {
        return expectedMinRows;
    }

    public void setExpectedMinRows(Long expectedMinRows) {
        this.expectedMinRows = expectedMinRows;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}