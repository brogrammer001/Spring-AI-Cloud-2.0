package com.mall.aichat.domain;

/** 记忆操作类型：对齐 Mem0 的 ADD/UPDATE/DELETE/NOOP */
public enum MemoryOperation {
    ADD,      // 新增一条记忆
    UPDATE,   // 用新内容替换某条旧记忆（需 targetId）
    DELETE,   // 删除某条旧记忆（需 targetId）
    NOOP      // 无需操作（重复/无意义）
}