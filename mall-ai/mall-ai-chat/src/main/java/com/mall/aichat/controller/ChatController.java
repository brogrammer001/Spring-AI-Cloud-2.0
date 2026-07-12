package com.mall.aichat.controller;

import com.mall.aichat.config.RagConfig;
import com.mall.aichat.config.VectorCompressionConfig;
import com.mall.common.core.utils.StringUtils;
import jakarta.annotation.Resource;
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

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private VectorCompressionConfig vectorCompressionConfig;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String question, @RequestParam(required = false) String conversationId) {
        // 1. 从向量库检索相关知识片段
        String relevantDoc = "";//ragConfig.retrieveContext(question);

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
}
