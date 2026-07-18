package com.mall.aichat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.aichat.config.RagConfig;
import com.mall.aichat.config.VectorCompressionConfig;
import com.mall.common.core.utils.StringUtils;
import com.mall.system.api.domain.ChatEventType;
import com.mall.system.api.domain.ChatStreamResponse;
import jakarta.annotation.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Resource(name = "mcpAsyncToolCallbacks")
    private ToolCallbackProvider toolCallbackProvider;

    @PostMapping(value = "/chat1", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat1(@RequestParam String question, @RequestParam(required = false) String conversationId) {
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

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> chatStream(@RequestParam String question, @RequestParam(required = false) String conversationId) {
        // 1. RAG 检索
        String relevantDoc = ragConfig.retrieveContext(question);

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
            .filter(StringUtils::isNotBlank)
            .map(content -> {
                try {
                    // 尝试将 AI 输出的文本直接解析为 ChatStreamResponse 对象
                    ChatStreamResponse parsedResponse = objectMapper.readValue(content, ChatStreamResponse.class);

                    // 如果解析成功，且 type 不是普通文本（说明是工具返回的指令，如 OPEN_MENU, CONFIRM 等）
                    // 则直接返回该对象，不再包裹！这样 SSE 就是 data: {"type":"OPEN_MENU"...}
                    if (parsedResponse.type() != ChatEventType.CONTENT) {
                        return parsedResponse;
                    }
                } catch (Exception e) {
                    // 解析失败（比如是普通的自然语言对话，或者格式不对），走默认的 CONTENT 包装逻辑
                    // 这里忽略异常，视为普通文本
                }
                // 默认情况：将自然语言文本包装为 CONTENT 类型
                // SSE 变成: data: {"type":"CONTENT", "content":"用户说的话..."}
                return new ChatStreamResponse(ChatEventType.CONTENT, content, null);
            })
            .map(response -> ServerSentEvent.<ChatStreamResponse>builder().data(response).build())
            .concatWith(Flux.just(ServerSentEvent.<ChatStreamResponse>builder()
                .data(new ChatStreamResponse(ChatEventType.DONE, "", null))
                .build()))
            .onErrorResume(e -> {
                log.error("Stream error", e);
                return Flux.just(ServerSentEvent.<ChatStreamResponse>builder()
                    .data(new ChatStreamResponse(ChatEventType.ERROR, "服务器内部错误: " + e.getMessage(), null))
                    .build());
            })
            .doOnComplete(() -> vectorCompressionConfig.checkAndCompressAsync(conversationId));
    }

}
