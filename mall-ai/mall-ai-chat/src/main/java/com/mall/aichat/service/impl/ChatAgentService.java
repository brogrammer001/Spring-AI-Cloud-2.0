package com.mall.aichat.service.impl;

import com.mall.aichat.config.AgentEventSinkManager;
import com.mall.aichat.constant.ChatConstants;
import com.mall.aichat.domain.ChatRequest;
import com.mall.aichat.domain.ChatStreamEvent;
import com.mall.common.core.utils.StringUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 聊天流式处理服务
 * <p>业务下沉：RAG 检索阶段与 LLM 流式阶段的全部事件统一推送到 Controller 传入的 sink，
 * 阻塞的检索调用运行在 boundedElastic 线程池，不占用请求线程</p>
 *
 * @author mall
 */
@Service
public class ChatAgentService {

    private static final Logger log = LoggerFactory.getLogger(ChatAgentService.class);

    private final ChatClient qwenChatClient;
    private final RagRetrieveContextService ragRetrieveContextService;
    private final AgentEventSinkManager agentEventSinkManager;

    @Value("classpath:/prompts/system-prompt-simplify.md")
    private org.springframework.core.io.Resource systemSimplifyPromptResource;

    @Value("${vectorstore.enabled}")
    private boolean vectorStoreEnabled;

    /** 系统提示词为静态资源，启动时加载一次，避免每次请求重复 IO */
    private String systemPromptTemplate;

    public ChatAgentService(@Qualifier("qwenChatClient") ChatClient qwenChatClient,
                            RagRetrieveContextService ragRetrieveContextService,
                            AgentEventSinkManager agentEventSinkManager) {
        this.qwenChatClient = qwenChatClient;
        this.ragRetrieveContextService = ragRetrieveContextService;
        this.agentEventSinkManager = agentEventSinkManager;
    }

    @PostConstruct
    public void init() {
        try (InputStream inputStream = systemSimplifyPromptResource.getInputStream()) {
            this.systemPromptTemplate = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load system prompt resource", ex);
        }
    }

    /**
     * 异步执行聊天流水线：RAG 检索阶段 → LLM 流式阶段，所有事件推送到 sink
     *
     * @return 流水线订阅句柄，客户端断连时用于取消后台任务
     */
    public Disposable process(Sinks.Many<ServerSentEvent<ChatStreamEvent>> sink, ChatRequest request) {
        String conversationId = request.getConversationId();
        AtomicReference<String> promptRef = new AtomicReference<>(systemPromptTemplate);
        return ragPhase(request, promptRef)
            .concatWith(Flux.defer(() -> llmPhase(request, promptRef.get())))
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
     * RAG 检索阶段：推送 start / success / empty 状态事件，阻塞检索运行在 boundedElastic
     * <p>组装好的系统提示词经 promptRef 传递给 LLM 阶段；检索失败降级为无上下文</p>
     */
    private Flux<ServerSentEvent<ChatStreamEvent>> ragPhase(ChatRequest request, AtomicReference<String> promptRef) {
        if (!vectorStoreEnabled) {
            return Flux.empty();
        }
        String conversationId = request.getConversationId();
        return Flux.concat(
            Flux.just(buildRagEvent(conversationId, ChatConstants.RAG_START)),
            Mono.fromCallable(() -> ragRetrieveContextService.retrieveContext(request.getQuestion()))
                .subscribeOn(Schedulers.boundedElastic())
                // 检索失败降级为空结果，不影响主对话流程
                .onErrorResume(ex -> {
                    log.error("RAG retrieve failed, conversation: {}", conversationId, ex);
                    return Mono.just("");
                })
                .flatMapMany(relevantDoc -> {
                    if (StringUtils.isNotEmpty(relevantDoc)) {
                        promptRef.set(systemPromptTemplate + ChatConstants.KNOWLEDGE_PREFIX + relevantDoc);
                        return Flux.just(buildRagEvent(conversationId, ChatConstants.RAG_SUCCESS));
                    }
                    return Flux.just(buildRagEvent(conversationId, ChatConstants.RAG_EMPTY));
                })
        );
    }

    /**
     * LLM 流式阶段：增量响应转 message 事件，并记录最后一次有效的 usage 统计
     */
    private Flux<ServerSentEvent<ChatStreamEvent>> llmPhase(ChatRequest request, String systemPrompt) {
        String conversationId = request.getConversationId();
        AtomicInteger idx = new AtomicInteger(0);

        return qwenChatClient.prompt()
            .user(request.getQuestion())
            .system(systemPrompt)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
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

    /**
     * 构建 RAG 状态事件（与原旁路推送保持一致：仅携带 data，不设置 SSE event 头）
     */
    private ServerSentEvent<ChatStreamEvent> buildRagEvent(String conversationId, String stage) {
        return ServerSentEvent.<ChatStreamEvent>builder()
            .data(ChatStreamEvent.ragRetrieve(conversationId, stage, 0))
            .build();
    }
}
