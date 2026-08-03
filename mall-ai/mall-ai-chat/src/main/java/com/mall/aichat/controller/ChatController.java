package com.mall.aichat.controller;

import com.mall.aichat.service.impl.RagRetrieveContextService;
import com.mall.aichat.service.impl.VectorCompressionService;
import com.mall.common.core.utils.StringUtils;
import com.mall.common.core.web.domain.AjaxResult;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @Autowired
    private RagRetrieveContextService ragConfig;

    @Autowired
    private VectorCompressionService vectorCompressionConfig;

    @Value("${vectorstore.enabled}")
    private boolean vectorStoreEnabled;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AjaxResult>> chatStream(@RequestParam String question, @RequestParam(required = false) String conversationId) {

        // 1. 构建请求
        var promptSpec = qwenChatClient.prompt()
            .user(question)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));

        if (vectorStoreEnabled) {
            // 2. RAG 检索
            String relevantDoc = ragConfig.retrieveContext(question);
            if (StringUtils.isNotEmpty(relevantDoc)) {
                promptSpec.system("参考知识：\n" + relevantDoc);
            }
        }

        return promptSpec.stream()
            // 1. 替换为 .chatResponse() 获取完整响应对象
            .chatResponse()
            // 2. 过滤掉大模型返回的空数据包 (无 result 或无 output)
            .filter(chatResponse -> chatResponse.getResult() != null)
            // 3. 使用 flatMap 处理文本和工具调用分支
            .flatMap(chatResponse -> {
                AssistantMessage output = chatResponse.getResult().getOutput();
                // 【正常分支】提取文本内容发给前端
                String textContent = output.getText();
                if (StringUtils.isNotEmpty(textContent)) {
                    return Flux.just(ServerSentEvent.<AjaxResult>builder()
                        .event("message") // 显式指定事件名为 message
                        .data(AjaxResult.success(textContent))
                        .build());
                }

                // 过滤掉既无工具调用又无文本的空 chunk
                return Flux.empty();
            })
            // 4. 增加超时控制，防止大模型卡死导致连接挂死
            .timeout(Duration.ofSeconds(600))
            // 5. 结束标记：显式指定事件名为 done
            .concatWith(Flux.just(
                ServerSentEvent.<AjaxResult>builder()
                    .event("done")
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
            .onErrorResume(e -> {
                log.error("Stream error for conversation {}: {}", conversationId, e.getMessage(), e);
                return Flux.just(ServerSentEvent.<AjaxResult>builder()
                    .event("error")
                    .data(AjaxResult.error("服务开小差了，请稍后再试")) // 对外脱敏
                    .build()
                );
            });
    }

}
