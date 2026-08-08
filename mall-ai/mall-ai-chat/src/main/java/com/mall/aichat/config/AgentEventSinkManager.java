package com.mall.aichat.config;

import com.mall.aichat.domain.ChatStreamEvent;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentEventSinkManager {

    // 每个会话一个独立的 Sink 通道
    private final Map<String, Sinks.Many<ServerSentEvent<ChatStreamEvent>>> sinks = new ConcurrentHashMap<>();

    public Sinks.Many<ServerSentEvent<ChatStreamEvent>> getOrCreate(String conversationId) {
        return sinks.computeIfAbsent(conversationId, k -> Sinks.many().multicast().onBackpressureBuffer());
    }

    // 推送结构化的思考事件
    public void emitThought(String conversationId, String toolName) {
        Sinks.Many<ServerSentEvent<ChatStreamEvent>> sink = sinks.get(conversationId);
        if (sink != null) {
            ServerSentEvent<ChatStreamEvent> sse = ServerSentEvent.<ChatStreamEvent>builder()
                .data(ChatStreamEvent.toolCall(conversationId, toolName, 0))
                .build();
            sink.tryEmitNext(sse);
        }
    }

    // 结束时清理通道
    public void complete(String conversationId) {
        Sinks.Many<ServerSentEvent<ChatStreamEvent>> sink = sinks.remove(conversationId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
