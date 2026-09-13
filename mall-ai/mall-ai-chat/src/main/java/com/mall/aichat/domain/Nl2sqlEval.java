package com.mall.aichat.domain;

import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * NL2SQL黄金评测集对象 nl2sql_eval
 * 
 * @author mall
 * @date 2026-09-10
 */
public class Nl2sqlEval extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用例ID */
    private Long id;

    /** 评测问题 */
    @Excel(name = "评测问题")
    private String question;

    /** 期望意图类型（QUERY/CHAT/CLARIFY） */
    @Excel(name = "期望意图类型", readConverterExp = "Q=UERY/CHAT/CLARIFY")
    private String expectedType;

    /** 期望SQL包含的片段（逗号分隔，大小写不敏感，全部满足才通过） */
    @Excel(name = "期望SQL包含的片段", readConverterExp = "逗=号分隔，大小写不敏感，全部满足才通过")
    private String expectedSqlContains;

    /** 期望SQL不包含的片段（逗号分隔） */
    @Excel(name = "期望SQL不包含的片段", readConverterExp = "逗=号分隔")
    private String expectedSqlNotContains;

    /** 期望最小结果行数（NULL则不断言行数） */
    @Excel(name = "期望最小结果行数", readConverterExp = "N=ULL则不断言行数")
    private Long expectedMinRows;

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

    public void setQuestion(String question) 
    {
        this.question = question;
    }

    public String getQuestion() 
    {
        return question;
    }

    public void setExpectedType(String expectedType) 
    {
        this.expectedType = expectedType;
    }

    public String getExpectedType() 
    {
        return expectedType;
    }

    public void setExpectedSqlContains(String expectedSqlContains) 
    {
        this.expectedSqlContains = expectedSqlContains;
    }

    public String getExpectedSqlContains() 
    {
        return expectedSqlContains;
    }

    public void setExpectedSqlNotContains(String expectedSqlNotContains) 
    {
        this.expectedSqlNotContains = expectedSqlNotContains;
    }

    public String getExpectedSqlNotContains() 
    {
        return expectedSqlNotContains;
    }

    public void setExpectedMinRows(Long expectedMinRows) 
    {
        this.expectedMinRows = expectedMinRows;
    }

    public Long getExpectedMinRows() 
    {
        return expectedMinRows;
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
            .append("question", getQuestion())
            .append("expectedType", getExpectedType())
            .append("expectedSqlContains", getExpectedSqlContains())
            .append("expectedSqlNotContains", getExpectedSqlNotContains())
            .append("expectedMinRows", getExpectedMinRows())
            .append("enabled", getEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
