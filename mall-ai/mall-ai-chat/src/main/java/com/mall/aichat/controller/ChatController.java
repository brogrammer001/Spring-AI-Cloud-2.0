package com.mall.aichat.controller;

import com.mall.common.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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


    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String question, @RequestParam(required = false) String conversationId) {
        return qwenChatClient.prompt()
            .user(question) // 只传用户问题
            .advisors(a -> {
                a.param(ChatMemory.CONVERSATION_ID, conversationId);
                a.param("userId", SecurityUtils.getUserId());
            })
            .stream()
            .content();
    }
}
