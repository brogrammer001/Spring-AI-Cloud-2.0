package com.mall.aichat.controller;

import com.mall.aichat.config.RagConfig;
import com.mall.aichat.config.VectorCompressionConfig;
import com.mall.common.core.utils.StringUtils;
import jakarta.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private static final Log log = LogFactory.getLog(ChatController.class);

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private VectorCompressionConfig vectorCompressionConfig;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestParam String question, @RequestParam(required = false) String conversationId) {
        // 1. RAG 检索
        String relevantDoc = "";//ragConfig.retrieveContext(question);

        // 2. 构建请求
        var promptSpec = qwenChatClient.prompt()
            .user(question)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));

        if (StringUtils.isNotEmpty(relevantDoc)) {
            promptSpec.system("参考知识：\n" + relevantDoc);
        }

        // 3. 流式处理与智能路由
        return promptSpec.stream()
            .content()
            // 直接将模型输出的内容（纯文本或JSON）作为 SSE 的 data 透传
            .map(content -> ServerSentEvent.<String>builder().data(content).build())
            // 结束标记：通过 SSE 的 event 字段标记为 done，data 中可以返回一个结束标识符
            .concatWith(Flux.just(
                ServerSentEvent.<String>builder().data("done").build()
            ))
            .onErrorResume(e -> {
                log.error("Stream error", e);
                // 错误标记：通过 SSE 的 event 字段标记为 error，前端可直接解析 data 获取错误信息
                return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("服务器内部错误: " + e.getMessage())
                    .build());
            });
            //.doOnComplete(() -> vectorCompressionConfig.checkAndCompressAsync(conversationId));
    }

}
