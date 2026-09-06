package com.mall.aichat.domain;

/** 记忆类型：对齐 Mem0 的 semantic(profile) / episodic(fact) 二分 */
public enum MemoryType {
    /** 用户画像：身份、偏好、长期属性。更新策略：覆盖 */
    PROFILE,

    /** 对话事实：事件、决定、临时意图。更新策略：追加 */
    FACT;

    /** 小写形式，用于写入向量库 metadata 与 prompt 展示 */
    public String lower() {
        return name().toLowerCase();
    }

    /** 从小写字符串解析，非法值默认 FACT（保守方向：事实不丢） */
    public static MemoryType fromLower(String value) {
        if (value == null) {
            return FACT;
        }
        return "profile".equalsIgnoreCase(value.strip()) ? PROFILE : FACT;
    }
}