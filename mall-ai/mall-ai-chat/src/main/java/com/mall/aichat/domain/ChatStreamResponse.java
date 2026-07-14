package com.mall.aichat.domain;

public record ChatStreamResponse(ChatEventType type, String content, Object data) {
}
