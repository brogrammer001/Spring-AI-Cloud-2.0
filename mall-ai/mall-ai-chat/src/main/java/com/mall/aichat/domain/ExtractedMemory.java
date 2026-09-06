package com.mall.aichat.domain;

/**
 * 记忆提取阶段的结构化输出（Phase 1 产物）。
 *
 * @param content  记忆文本（以"用户"开头的原子事实短句）
 * @param type     记忆类型（PROFILE / FACT）
 */
public record ExtractedMemory(String content, MemoryType type) {
}
