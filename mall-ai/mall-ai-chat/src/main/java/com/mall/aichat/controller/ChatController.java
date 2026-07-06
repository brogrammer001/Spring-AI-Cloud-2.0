package com.mall.aichat.controller;

import com.mall.aichat.config.RagConfig;
import com.mall.common.core.utils.StringUtils;
import com.mall.common.security.utils.SecurityUtils;
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

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String question, @RequestParam(required = false) String conversationId) {
        // 1. 从向量库检索相关知识片段
        String relevantDoc = ragConfig.retrieveContext(question);
        if (StringUtils.isNotEmpty(relevantDoc)) {
            // 直接拼接即可
            relevantDoc = "\n\n【参考知识】\n" + relevantDoc;
        }

        String promptContent = relevantDoc;
        return qwenChatClient.prompt()
            .system(s -> s.text(promptContent))
            .user(question)
            .advisors(a -> {
                a.param(ChatMemory.CONVERSATION_ID, conversationId);
                a.param("userId", SecurityUtils.getUserId());
            })
            .stream()
            .content();
    }
}
