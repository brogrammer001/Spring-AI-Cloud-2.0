package com.mall.system.api.domain;

import java.io.Serializable;

/**
 * NL2SQL维度定义对象 nl2sql_dimension
 * <p>
 * Feign 远程调用 DTO，与 mall-ai-chat 的 com.mall.aichat.domain.Nl2sqlDimension 字段对齐。
 * 仅包含 NL2SQL 执行链路所需的业务字段，省略 BaseEntity 审计字段。
 */
public class Nl2sqlDimensionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 维度ID */
    private Long id;

    /** 维度名，如订单状态 */
    private String dimName;

    /** 所属表名 */
    private String tableName;

    /** 所属列名 */
    private String columnName;

    /** 同义词，逗号分隔 */
    private String synonyms;

    /** 合法枚举值及含义：[{"value":"0","meaning":"待支付"},{"value":"1","meaning":"已支付"}] */
    private String dimValues;

    /** 是否启用（1是 0否） */
    private String enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDimName() {
        return dimName;
    }

    public void setDimName(String dimName) {
        this.dimName = dimName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getDimValues() {
        return dimValues;
    }

    public void setDimValues(String dimValues) {
        this.dimValues = dimValues;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }
}