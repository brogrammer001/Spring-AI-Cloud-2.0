package com.mall.system.api.domain;

import java.io.Serializable;

/**
 * NL2SQL业务规则对象 nl2sql_business_rule
 * <p>
 * Feign 远程调用 DTO，与 mall-ai-chat 的 com.mall.aichat.domain.Nl2sqlBusinessRule 字段对齐。
 * 仅包含 NL2SQL 执行链路所需的业务字段，省略 BaseEntity 审计字段。
 */
public class Nl2sqlBusinessRuleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long id;

    /** 规则名 */
    private String ruleName;

    /** "有效用户"= status=0 AND del_flag=0；"本月"= 本月1日至今 */
    private String ruleContent;

    /** 适用关键词，逗号分隔，为空则全局生效 */
    private String appliesTo;

    /** 是否启用（1是 0否） */
    private String enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleContent() {
        return ruleContent;
    }

    public void setRuleContent(String ruleContent) {
        this.ruleContent = ruleContent;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}