package com.mall.aichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.aichat.config.RagConfig;
import com.mall.aichat.config.ToolResultHolder;
import com.mall.aichat.config.VectorCompressionConfig;
import com.mall.aichat.domain.ChatEventType;
import com.mall.aichat.domain.ChatStreamResponse;
import com.mall.common.core.utils.StringUtils;
import jakarta.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

    @Autowired
    private ToolResultHolder resultHolder;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(value = "/chat2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String question, @RequestParam(required = false) String conversationId) {
        // 1. 从向量库检索相关知识片段
        String relevantDoc = ragConfig.retrieveContext(question);

        // 2. 构建提示词请求
        // 注意：这里使用了链式调用，通过 .system() 设置系统上下文，.user() 保持用户原始输入
        var promptSpec = qwenChatClient.prompt()
            .user(question); // 【关键修改】用户输入仅包含原始问题，记忆组件将只记录这部分

        // 如果存在参考知识，将其作为系统提示注入，而不是拼接在用户输入中
        if (StringUtils.isNotEmpty(relevantDoc)) {
            String systemContext = "请根据以下参考知识回答我的问题。\n\n【参考知识】\n" + relevantDoc;
            // 将参考知识放入 System Prompt，对模型起到引导作用，但不会被记忆组件记录为用户发言
            promptSpec.system(systemContext);
        }

        return promptSpec
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .stream()
            .content()
            .doOnComplete(() -> vectorCompressionConfig.checkAndCompressAsync(conversationId));
    }

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> chatStream(
        @RequestParam String question,
        @RequestParam(required = false) String conversationId) {

        //知识库检索
        String relevantDoc = ragConfig.retrieveContext(question);

        var promptSpec = qwenChatClient.prompt()
            .user(question) // 仅原始问题进入记忆
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));

        if (StringUtils.isNotEmpty(relevantDoc)) {
            promptSpec.system("参考知识：\n" + relevantDoc);
        }

        // 1. 获取纯净的文本流 (绕过 getText() 为空的问题)
        Flux<String> textFlux = promptSpec.stream().content();

        // 2. 如果工具被触发了，替换为业务 JSON，并忽略后续的 textFlux
        if (resultHolder.isHasAction()) {
            String bizJson = toJsonString(new ChatStreamResponse(
                resultHolder.getActionType(),
                resultHolder.getConfirmDesc(),
                resultHolder.getActionData()
            ));
            return Flux.just(bizJson)
                .concatWith(Flux.just(toJsonString(new ChatStreamResponse(ChatEventType.DONE, "", null))))
                .doOnComplete(() -> resultHolder.clear());
        }

        // 3. 正常的聊天流处理
        return textFlux
            .filter(StringUtils::isNotEmpty) // 自动过滤空心跳包
            .map(text -> new ChatStreamResponse(ChatEventType.CONTENT, text, null))
            .map(this::toJsonString)
            .concatWith(Flux.just(toJsonString(new ChatStreamResponse(ChatEventType.DONE, "", null))))
            .onErrorResume(e -> {
                log.error("流处理异常", e);
                return Flux.just(toJsonString(new ChatStreamResponse(ChatEventType.ERROR, "服务器内部错误", null)));
            })
            .doOnComplete(() -> {
                resultHolder.clear();
                vectorCompressionConfig.checkAndCompressAsync(conversationId);
            });
    }

    /**
     * 使用 Jackson 将实体类序列化为 JSON 字符串
     */
    private String toJsonString(ChatStreamResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            try {
                // 降级返回简单的错误 JSON
                return objectMapper.writeValueAsString(new ChatStreamResponse(ChatEventType.ERROR, "JSON序列化失败", null));
            } catch (Exception ignored) {
                return "{\"type\":\"error\",\"content\":\"JSON序列化失败\"}";
            }
        }
    }
}
