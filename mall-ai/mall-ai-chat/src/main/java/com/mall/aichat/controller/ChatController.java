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
        String relevantDoc = ragConfig.retrieveContext(question);

        // 构建完整的 User Prompt
        String promptContent = question;
        if (StringUtils.isNotEmpty(relevantDoc)) {
            promptContent = "请根据以下参考知识回答我的问题。\n\n【参考知识】\n" + relevantDoc + "\n\n【我的问题】\n" + question;
        }

        return qwenChatClient.prompt()
            .user(promptContent)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .stream()
            .content()
            .doOnComplete(() -> vectorCompressionConfig.checkAndCompressAsync(conversationId));
    }
}
