package com.mall.aichat.domain;

import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * NL2SQL业务规则对象 nl2sql_business_rule
 * 
 * @author mall
 * @date 2026-09-10
 */
public class Nl2sqlBusinessRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long id;

    /** 规则名 */
    @Excel(name = "规则名")
    private String ruleName;

    /** "有效用户"= status=0 AND del_flag=0；"本月"= 本月1日至今 */
    @Excel(name = "有效用户= status=0 AND del_flag=0；本月= 本月1日至今")
    private String ruleContent;

    /** 适用关键词，逗号分隔，为空则全局生效 */
    @Excel(name = "适用关键词，逗号分隔，为空则全局生效")
    private String appliesTo;

    /** 是否启用（1是 0否） */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enabled;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setRuleName(String ruleName) 
    {
        this.ruleName = ruleName;
    }

    public String getRuleName() 
    {
        return ruleName;
    }

    public void setRuleContent(String ruleContent) 
    {
        this.ruleContent = ruleContent;
    }

    public String getRuleContent() 
    {
        return ruleContent;
    }

    public void setAppliesTo(String appliesTo) 
    {
        this.appliesTo = appliesTo;
    }

    public String getAppliesTo() 
    {
        return appliesTo;
    }

    public void setEnabled(String enabled) 
    {
        this.enabled = enabled;
    }

    public String getEnabled() 
    {
        return enabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("ruleName", getRuleName())
            .append("ruleContent", getRuleContent())
            .append("appliesTo", getAppliesTo())
            .append("enabled", getEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
