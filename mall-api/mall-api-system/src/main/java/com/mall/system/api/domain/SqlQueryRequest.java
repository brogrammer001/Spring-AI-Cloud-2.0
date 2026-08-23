package com.mall.system.api.domain;

import java.io.Serializable;

/**
 * SQL查询请求DTO
 * <p>
 * 使用DTO封装SQL字符串，避免Feign直接传输String时
 * StringHttpMessageConverter 默认使用 ISO-8859-1 编码导致中文被转义为 '?'
 */
public class SqlQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** SQL语句 */
    private String sql;

    public SqlQueryRequest() {
    }

    public SqlQueryRequest(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}