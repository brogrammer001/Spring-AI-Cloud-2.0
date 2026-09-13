package com.mall.aichat.domain;

import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * NL2SQL维度定义对象 nl2sql_dimension
 * 
 * @author mall
 * @date 2026-09-10
 */
public class Nl2sqlDimension extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 维度ID */
    private Long id;

    /** 维度名，如订单状态 */
    @Excel(name = "维度名，如订单状态")
    private String dimName;

    /** 所属表名 */
    @Excel(name = "所属表名")
    private String tableName;

    /** 所属列名 */
    @Excel(name = "所属列名")
    private String columnName;

    /** 同义词，逗号分隔 */
    @Excel(name = "同义词，逗号分隔")
    private String synonyms;

    /** 合法枚举值及含义：[{"value":"0","meaning":"待支付"},{"value":"1","meaning":"已支付"}] */
    @Excel(name = "合法枚举值及含义：[{value:0,meaning:待支付},{value:1,meaning:已支付}]")
    private String dimValues;

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

    public void setDimName(String dimName) 
    {
        this.dimName = dimName;
    }

    public String getDimName() 
    {
        return dimName;
    }

    public void setTableName(String tableName) 
    {
        this.tableName = tableName;
    }

    public String getTableName() 
    {
        return tableName;
    }

    public void setColumnName(String columnName) 
    {
        this.columnName = columnName;
    }

    public String getColumnName() 
    {
        return columnName;
    }

    public void setSynonyms(String synonyms) 
    {
        this.synonyms = synonyms;
    }

    public String getSynonyms() 
    {
        return synonyms;
    }

    public void setDimValues(String dimValues) 
    {
        this.dimValues = dimValues;
    }

    public String getDimValues() 
    {
        return dimValues;
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
            .append("dimName", getDimName())
            .append("tableName", getTableName())
            .append("columnName", getColumnName())
            .append("synonyms", getSynonyms())
            .append("dimValues", getDimValues())
            .append("enabled", getEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
