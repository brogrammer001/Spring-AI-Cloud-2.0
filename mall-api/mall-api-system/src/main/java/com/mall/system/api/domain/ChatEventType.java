package com.mall.system.api.domain;

public enum ChatEventType {
    CONTENT,       // 打字机文本流
    OPEN_MENU,   // 导航意图
    DATA_TABLE, // 数据查询意图
    CONFIRM,       // 新增/修改/删除确认意图
    DONE,             // 流结束标记
    CHART,             // 图表
    ERROR;           // 错误信息
}
