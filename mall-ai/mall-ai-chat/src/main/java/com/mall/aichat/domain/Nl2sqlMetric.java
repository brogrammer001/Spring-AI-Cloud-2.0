package com.mall.aichat.domain;

import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * NL2SQL指标定义对象 nl2sql_metric
 * 
 * @author mall
 * @date 2026-09-10
 */
public class Nl2sqlMetric extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 指标ID */
    private Long id;

    /** 指标名，如复购率 */
    @Excel(name = "指标名，如复购率")
    private String metricName;

    /** 同义词，逗号分隔：回购率,重复购买率 */
    @Excel(name = "同义词，逗号分隔：回购率,重复购买率")
    private String synonyms;

    /** SQL表达式模板：COUNT(DISTINCT CASE WHEN 下单次数>=2 THEN user_id END) / COUNT(DISTINCT user_id) */
    @Excel(name = "SQL表达式模板：COUNT(DISTINCT CASE WHEN 下单次数>=2 THEN user_id END) / COUNT(DISTINCT user_id)")
    private String metricExpr;

    /** 默认聚合方式 */
    @Excel(name = "默认聚合方式")
    private String aggDefault;

    /** 单位：% / 元 / 人 */
    @Excel(name = "单位：% / 元 / 人")
    private String unit;

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

    public void setMetricName(String metricName) 
    {
        this.metricName = metricName;
    }

    public String getMetricName() 
    {
        return metricName;
    }

    public void setSynonyms(String synonyms) 
    {
        this.synonyms = synonyms;
    }

    public String getSynonyms() 
    {
        return synonyms;
    }

    public void setMetricExpr(String metricExpr) 
    {
        this.metricExpr = metricExpr;
    }

    public String getMetricExpr() 
    {
        return metricExpr;
    }

    public void setAggDefault(String aggDefault) 
    {
        this.aggDefault = aggDefault;
    }

    public String getAggDefault() 
    {
        return aggDefault;
    }

    public void setUnit(String unit) 
    {
        this.unit = unit;
    }

    public String getUnit() 
    {
        return unit;
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
            .append("metricName", getMetricName())
            .append("synonyms", getSynonyms())
            .append("metricExpr", getMetricExpr())
            .append("aggDefault", getAggDefault())
            .append("unit", getUnit())
            .append("enabled", getEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
