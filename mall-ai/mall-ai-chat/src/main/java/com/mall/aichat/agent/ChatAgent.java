package com.mall.aichat.agent;

import com.mall.aichat.config.AgentEventSinkManager;
import com.mall.aichat.constant.ChatConstants;
import com.mall.aichat.domain.ChatRequest;
import com.mall.aichat.domain.ChatStreamEvent;
import com.mall.aichat.service.impl.ChatAgentService;
import com.mall.aichat.service.impl.VectorCompressionService;
import com.mall.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/ai")
public class ChatAgent {

    private static final Logger log = LoggerFactory.getLogger(ChatAgent.class);

    private final ChatAgentService chatAgentService;
    private final VectorCompressionService vectorCompressionService;
    private final AgentEventSinkManager agentEventSinkManager;

    @Value("${vectorstore.enabled}")
    private boolean vectorStoreEnabled;

    /** 流式超时时间（秒），防止大模型卡死导致连接挂死 */
    @Value("${ai.chat.stream-timeout-seconds:600}")
    private long streamTimeoutSeconds;

    public ChatAgent(ChatAgentService chatAgentService,
                     VectorCompressionService vectorCompressionService,
                     AgentEventSinkManager agentEventSinkManager) {
        this.chatAgentService = chatAgentService;
        this.vectorCompressionService = vectorCompressionService;
        this.agentEventSinkManager = agentEventSinkManager;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamEvent>> chatStream(@RequestParam String question,
                                                             @RequestParam(required = false) String conversationId) {
        if (StringUtils.isBlank(question)) {
            return Flux.just(buildErrorEvent(ChatConstants.ERROR_INVALID_PARAM, "问题内容不能为空"));
        }

        // conversationId 是 Sink 通道与 ChatMemory 的必要参数，缺失时自动生成，避免 null key 引发 NPE
        String convId = StringUtils.isNotEmpty(conversationId) ? conversationId : UUID.randomUUID().toString();

        ChatRequest request = ChatRequest.builder()
            .question(question)
            .conversationId(convId)
            .build();

        // 单播通道：订阅前推送的事件（如 RAG start 状态）会进入缓冲区，不会丢失；
        // 注册到管理器供 FullHistoryChatMemoryAdvisor 推送 tool_call 旁路事件
        Sinks.Many<ServerSentEvent<ChatStreamEvent>> sink =
            agentEventSinkManager.register(convId, Sinks.many().unicast().onBackpressureBuffer());

        // 业务全在 Service 里异步执行，事件统一推送到 sink
        Disposable task = chatAgentService.process(sink, request);
        return decorate(sink.asFlux(), request, task);
    }

    /**
     * SSE 流统一装饰：出口内容过滤、超时控制、结束标记、断连清理、异常脱敏
     */
    private Flux<ServerSentEvent<ChatStreamEvent>> decorate(Flux<ServerSentEvent<ChatStreamEvent>> flux,
                                                            ChatRequest request, Disposable task) {
        String convId = request.getConversationId();
        return flux
            // 1. 出口内容过滤：终止事件必达、状态事件放行、增量分片必须有正文，避免无效 SSE 推给前端
            .filter(this::isValidEvent)
            // 2. 超时控制，防止大模型卡死导致连接挂死
            .timeout(Duration.ofSeconds(streamTimeoutSeconds))
            // 3. 结束标记：defer 保证在流结束时才读取 usage
            .concatWith(Flux.defer(() -> Flux.just(buildEndEvent(request))))
            // 4. 统一清理：complete / error / cancel / timeout 任一信号都会触发
            .doFinally(signal -> {
                if (signal == SignalType.CANCEL) {
                    log.warn("Client disconnected prematurely, conversation: {}", convId);
                } else {
                    log.info("Stream finished with signal: {} for conversation: {}", signal, convId);
                }
                task.dispose();                          // 终止后台流水线（已完成时为 no-op）
                agentEventSinkManager.complete(convId);  // 清理注册表（幂等）
                if (vectorStoreEnabled) {
                    // 异步压缩会话记忆，失败不影响主流程
                    try {
                        vectorCompressionService.checkAndCompressAsync(convId);
                    } catch (Exception ex) {
                        log.error("Async compress memory failed for conversation: {}", convId, ex);
                    }
                }
            })
            // 5. 异常处理：显式指定事件名为 error，并将底层异常脱敏
            .onErrorResume(e -> {
                log.error("Chat stream failed, conversation: {}", convId, e);   // ★ 记录堆栈
                if (e instanceof TimeoutException) {
                    return Flux.just(buildErrorEvent(ChatConstants.ERROR_INTERNAL, "回答超时，请稍后重试"));
                }
                return Flux.just(buildErrorEvent(ChatConstants.ERROR_INTERNAL, "服务开小差了"));
            });
    }

    /**
     * 出口内容过滤（三态判定）：
     * <p>1. message_end / error 为终止事件，必须送达前端；
     * 2. tool_call / rag_retrieve 等状态事件直接放行；
     * 3. message 增量分片必须有正文，拦截 result 非空但 text 为空的无效帧</p>
     */
    private boolean isValidEvent(ServerSentEvent<ChatStreamEvent> sse) {
        ChatStreamEvent data = sse.data();
        if (data == null) {
            return false;
        }
        String event = data.event();
        if (ChatConstants.EVENT_MESSAGE_END.equals(event) || ChatConstants.EVENT_ERROR.equals(event)) {
            return true;
        }
        if (ChatConstants.EVENT_MESSAGE.equals(event)) {
            return StringUtils.isNotBlank(data.content());
        }
        // 思考/工具等状态事件放行
        return true;
    }

    /**
     * 构建结束事件，携带 usage 统计（无则返回空 map）
     */
    private ServerSentEvent<ChatStreamEvent> buildEndEvent(ChatRequest request) {
        Usage usage = request.getUsageRef().get();
        Map<String, Object> usageMeta = usage != null
            ? Map.of(
                "promptTokens", usage.getPromptTokens(),
                "completionTokens", usage.getCompletionTokens(),
                "totalTokens", usage.getTotalTokens())
            : Map.of();
        return ServerSentEvent.<ChatStreamEvent>builder()
            .event(ChatConstants.EVENT_MESSAGE_END)
            .data(ChatStreamEvent.end(request.getConversationId(), request.getMessageId(), Map.of("usage", usageMeta)))
            .build();
    }

    /**
     * 构建脱敏后的错误事件
     */
    private ServerSentEvent<ChatStreamEvent> buildErrorEvent(String code, String message) {
        return ServerSentEvent.<ChatStreamEvent>builder()
            .event(ChatConstants.EVENT_ERROR)
            .data(ChatStreamEvent.error(code, message))
            .build();
    }

}
