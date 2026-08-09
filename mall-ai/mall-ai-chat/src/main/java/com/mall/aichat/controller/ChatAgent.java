package com.mall.aichat.controller;

import com.mall.aichat.config.AgentEventSinkManager;
import com.mall.aichat.domain.ChatStreamEvent;
import com.mall.aichat.service.impl.RagRetrieveContextService;
import com.mall.aichat.service.impl.VectorCompressionService;
import com.mall.common.core.utils.StringUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/ai")
public class ChatAgent {

    private static final Logger log = LoggerFactory.getLogger(ChatAgent.class);

    @Value("classpath:/prompts/system-prompt-simplify.md")
    private org.springframework.core.io.Resource systemSimplifyPromptResource;

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @Autowired
    private RagRetrieveContextService ragConfig;

    @Autowired
    private VectorCompressionService vectorCompressionConfig;

    @Autowired
    private AgentEventSinkManager agentEventSinkManager;

    @Value("${vectorstore.enabled}")
    private boolean vectorStoreEnabled;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamEvent>> chatStream(@RequestParam String question, @RequestParam(required = false) String conversationId) {

        // ★ 提前获取该会话的旁路 Sink，以便在 RAG 检索时推送状态
        Sinks.Many<ServerSentEvent<ChatStreamEvent>> thinkingSink = agentEventSinkManager.getOrCreate(conversationId);

        // 1. 构建请求
        var promptSpec = qwenChatClient.prompt()
            .user(question)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));


        try (InputStream inputStream = systemSimplifyPromptResource.getInputStream()) {
            String systemSimplifyPrompt = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
            if (vectorStoreEnabled) {
                // ★ 推送：正在检索知识库
                agentEventSinkManager.ragEmitThought(conversationId, "start");
                // 2. RAG 检索
                String relevantDoc = ragConfig.retrieveContext(question);
                if (StringUtils.isNotEmpty(relevantDoc)) {
                    systemSimplifyPrompt += "\n参考知识：\n" + relevantDoc;
                    agentEventSinkManager.ragEmitThought(conversationId, "success");
                }else {
                    agentEventSinkManager.ragEmitThought(conversationId, "empty");
                }
            }
            promptSpec.system(systemSimplifyPrompt);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read resource", ex);
        }

        AtomicInteger idx = new AtomicInteger(0);
        String messageId = UUID.randomUUID().toString();

        Flux<ServerSentEvent<ChatStreamEvent>> serverSentEventFlux = promptSpec.stream()
            // 1. 替换为 .chatResponse() 获取完整响应对象
            .chatResponse()
            // 2. 过滤掉大模型返回的空数据包 (无 result 或无 output)
            .filter(chatResponse -> chatResponse.getResult() != null)
            // 3. 使用 flatMap 处理文本和工具调用分支
            .flatMap(r -> {
                AssistantMessage output = r.getResult().getOutput();
                String text = output.getText();

                if (StringUtils.isEmpty(text)) return Flux.empty();
                return Flux.just(ServerSentEvent.<ChatStreamEvent>builder()
                    .data(ChatStreamEvent.chunk(conversationId, text, idx.getAndIncrement()))
                    .build());
            })
            .doFinally(sig -> {
                log.info("Main LLM stream finished with signal: {}, closing thinking sink.", sig);
                agentEventSinkManager.complete(conversationId);
            });
        // ★ 合并：主流 + 旁路思考流
        return Flux.merge(serverSentEventFlux, thinkingSink.asFlux())
            // 4. 增加超时控制，防止大模型卡死导致连接挂死
            .timeout(Duration.ofSeconds(600))
            // 5. 结束标记：显式指定事件名为 done
            .concatWith(Flux.just(
                ServerSentEvent.<ChatStreamEvent>builder()
                    .data(ChatStreamEvent.end(conversationId, messageId,
                        Map.of("usage", Map.of())))  // 这里可以从最后 ChatResponse 拿 usage
                    .build()
            ))
            // 6. 使用 doFinally 替代 doOnComplete
            .doFinally(signal -> {
                log.info("Stream finished with signal: {} for conversation: {}", signal, conversationId);
                if (vectorStoreEnabled) {
                    //向量压缩
                    try {
                        vectorCompressionConfig.checkAndCompressAsync(conversationId);
                    } catch (Exception ex) {
                        log.error("Async compress memory failed for conversation: {}", conversationId, ex);
                    }
                }
            })
            // 7. 客户端主动断开处理
            .doOnCancel(() -> log.info("Client disconnected prematurely for conversation: {}", conversationId))
            // 8. 异常处理：显式指定事件名为 error，并将底层异常脱敏
            .onErrorResume(e -> Flux.just(
                ServerSentEvent.<ChatStreamEvent>builder()
                    .data(ChatStreamEvent.error("INTERNAL_ERROR", "服务开小差了"))
                    .build()
            ));
    }

}
