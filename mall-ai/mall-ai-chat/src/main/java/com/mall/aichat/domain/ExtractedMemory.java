package com.mall.aichat.domain;

/**
 * 记忆提取阶段的结构化输出（Phase 1 产物）。
 *
 * @param content    记忆文本（原子事实短句，格式由 prompt 约束）
 * @param type       记忆类型（PROFILE / FACT）
 * @param importance 重要性（0~1），用于检索排序加权
 */
public record ExtractedMemory(String content, MemoryType type, double importance) {
}
