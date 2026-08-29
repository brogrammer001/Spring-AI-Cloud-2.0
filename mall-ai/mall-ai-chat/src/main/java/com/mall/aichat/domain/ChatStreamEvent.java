package com.mall.aichat.domain;

import com.mall.aichat.constant.ChatConstants;

import java.util.Map;

public record ChatStreamEvent(
    String event,            // message | message_end | error | ping
    String messageId,        // 大模型消息 ID
    String conversationId,
    String content,          // 增量内容（仅 message 事件有值）
    Integer index,           // 包序号，前端去重/排序
    Map<String, Object> metadata  // 仅 message_end 携带 usage 等
) {
    public static ChatStreamEvent chunk(String convId, String content, int idx) {
        return new ChatStreamEvent(ChatConstants.EVENT_MESSAGE, null, convId, content, idx, null);
    }
    public static ChatStreamEvent toolCall(String convId, String content, int idx) {
        return new ChatStreamEvent(ChatConstants.EVENT_TOOL_CALL, null, convId, content, idx, null);
    }
    public static ChatStreamEvent ragRetrieve(String convId, String content, int idx) {
        return new ChatStreamEvent(ChatConstants.EVENT_RAG_RETRIEVE, null, convId, content, idx, null);
    }
    public static ChatStreamEvent end(String convId, String msgId, Map<String,Object> meta) {
        return new ChatStreamEvent(ChatConstants.EVENT_MESSAGE_END, msgId, convId, null, null, meta);
    }
    public static ChatStreamEvent error(String code, String msg) {
        return new ChatStreamEvent(ChatConstants.EVENT_ERROR, null, null, null, null, Map.of("code", code, "message", msg));
    }
}
