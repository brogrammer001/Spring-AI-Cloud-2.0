package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;

public class SqlQueryBo {

    @NotBlank(message = "查询问题不能为空")
    @JsonPropertyDescription("用户的自然语言查询问题，例如：查询最近一个月销售额最高的5个产品")
    private String question;

    @JsonPropertyDescription("指定查询的表名列表，可选，不指定则自动识别所有表")
    private String[] tableNames;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String[] getTableNames() {
        return tableNames;
    }

    public void setTableNames(String[] tableNames) {
        this.tableNames = tableNames;
    }
}