package com.mall.system.api.domain;

import java.io.Serializable;

/**
 * NL2SQL指标定义对象 nl2sql_metric
 * <p>
 * Feign 远程调用 DTO，与 mall-ai-chat 的 com.mall.aichat.domain.Nl2sqlMetric 字段对齐。
 * 仅包含 NL2SQL 执行链路所需的业务字段，省略 BaseEntity 审计字段。
 */
public class Nl2sqlMetricVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 指标ID */
    private Long id;

    /** 指标名，如复购率 */
    private String metricName;

    /** 同义词，逗号分隔：回购率,重复购买率 */
    private String synonyms;

    /** SQL表达式模板 */
    private String metricExpr;

    /** 默认聚合方式 */
    private String aggDefault;

    /** 单位：% / 元 / 人 */
    private String unit;

    /** 是否启用（1是 0否） */
    private String enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getMetricExpr() {
        return metricExpr;
    }

    public void setMetricExpr(String metricExpr) {
        this.metricExpr = metricExpr;
    }

    public String getAggDefault() {
        return aggDefault;
    }

    public void setAggDefault(String aggDefault) {
        this.aggDefault = aggDefault;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}