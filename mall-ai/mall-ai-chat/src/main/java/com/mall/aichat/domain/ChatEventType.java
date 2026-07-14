package com.mall.aichat.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChatEventType {
    CONTENT("CONTENT"),       // 打字机文本流
    OPEN_MENU("OPEN_MENU"),   // 导航意图
    DATA_TABLE("DATA_TABLE"), // 数据查询意图
    CONFIRM("CONFIRM"),       // 新增/修改/删除确认意图
    DONE("DONE"),             // 流结束标记
    ERROR("ERROR");           // 错误信息

    private final String code;

    ChatEventType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
