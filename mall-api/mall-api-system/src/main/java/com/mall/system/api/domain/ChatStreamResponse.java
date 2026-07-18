package com.mall.system.api.domain;

public record ChatStreamResponse(ChatEventType type, String content, Object data) {
}
