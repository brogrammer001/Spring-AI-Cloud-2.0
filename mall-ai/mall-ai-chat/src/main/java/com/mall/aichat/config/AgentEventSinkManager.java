package com.mall.aichat.config;

import com.mall.aichat.domain.ChatStreamEvent;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentEventSinkManager {

    // 每个会话一个独立的 Sink 通道（主流事件与 tool_call 旁路事件共用）
    private final Map<String, Sinks.Many<ServerSentEvent<ChatStreamEvent>>> sinks = new ConcurrentHashMap<>();

    /**
     * 注册会话的 Sink 通道，供 advisor 等组件按 conversationId 推送旁路事件
     */
    public Sinks.Many<ServerSentEvent<ChatStreamEvent>> register(String conversationId,
                                                                 Sinks.Many<ServerSentEvent<ChatStreamEvent>> sink) {
        sinks.put(conversationId, sink);
        return sink;
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

    // 结束时清理通道（幂等：重复调用或通道已移除时为 no-op）
    public void complete(String conversationId) {
        Sinks.Many<ServerSentEvent<ChatStreamEvent>> sink = sinks.remove(conversationId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    // 异常时清理通道，并向下游传播错误（由调用方的 onErrorResume 转成脱敏的 error 事件）
    public void completeWithError(String conversationId, Throwable error) {
        Sinks.Many<ServerSentEvent<ChatStreamEvent>> sink = sinks.remove(conversationId);
        if (sink != null) {
            sink.tryEmitError(error);
        }
    }
}
