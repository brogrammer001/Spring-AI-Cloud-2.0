package com.mall.aichat.service.impl;

import com.mall.aichat.config.AgentEventSinkManager;
import com.mall.aichat.constant.ChatConstants;
import com.mall.aichat.domain.ChatRequest;
import com.mall.aichat.domain.ChatStreamEvent;
import com.mall.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 聊天流式处理服务
 * <p>RAG 检索阶段已下沉到 {@code RagContextQueryAdvisor}（在 LLM 请求链路内执行），
 * 本服务仅负责 LLM 流式阶段：增量响应转 message 事件，并记录最后一次有效的 usage 统计</p>
 *
 * @author mall
 */
@Service
public class ChatAgentService {

    private static final Logger log = LoggerFactory.getLogger(ChatAgentService.class);

    private final ChatClient qwenChatClient;
    private final AgentEventSinkManager agentEventSinkManager;

    public ChatAgentService(@Qualifier("qwenChatClient") ChatClient qwenChatClient,
                            AgentEventSinkManager agentEventSinkManager) {
        this.qwenChatClient = qwenChatClient;
        this.agentEventSinkManager = agentEventSinkManager;
    }

    /**
     * 异步执行聊天流水线：LLM 流式阶段，所有事件推送到 sink
     * <p>RAG 检索由 {@code RagContextQueryAdvisor} 在请求链路内完成，并推送 rag_retrieve 状态事件</p>
     *
     * @return 流水线订阅句柄，客户端断连时用于取消后台任务
     */
    public Disposable process(Sinks.Many<ServerSentEvent<ChatStreamEvent>> sink, ChatRequest request) {
        String conversationId = request.getConversationId();
        return llmPhase(request)
            .subscribe(
                sink::tryEmitNext,
                err -> {
                    log.error("Chat pipeline failed, conversation: {}", conversationId, err);
                    agentEventSinkManager.completeWithError(conversationId, err);
                },
                () -> agentEventSinkManager.complete(conversationId)
            );
    }

    /**
     * LLM 流式阶段：增量响应转 message 事件，并记录最后一次有效的 usage 统计
     */
    private Flux<ServerSentEvent<ChatStreamEvent>> llmPhase(ChatRequest request) {
        String conversationId = request.getConversationId();
        AtomicInteger idx = new AtomicInteger(0);

        return qwenChatClient.prompt()
            .user(request.getQuestion())
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)
                // ★ 长期记忆作用域：VectorStoreChatMemoryAdvisor 据此按 userId 跨会话检索/写入
                .param(ChatConstants.CTX_USER_ID, request.getUserId()))
            .stream()
            .chatResponse()
            // 过滤掉大模型返回的空数据包 (无 result 或无 output)
            .filter(chatResponse -> chatResponse.getResult() != null)
            .flatMap(r -> {
                // 记录最后一个响应携带的 usage，供 message_end 事件使用
                Usage usage = r.getMetadata().getUsage();
                if (usage.getTotalTokens() > 0) {
                    request.getUsageRef().set(usage);
                }
                AssistantMessage output = r.getResult().getOutput();
                String text = output.getText();
                if (StringUtils.isEmpty(text)) {
                    return Flux.empty();
                }
                return Flux.just(ServerSentEvent.<ChatStreamEvent>builder()
                    .event(ChatConstants.EVENT_MESSAGE)
                    .data(ChatStreamEvent.chunk(conversationId, text, idx.getAndIncrement()))
                    .build());
            });
    }
}