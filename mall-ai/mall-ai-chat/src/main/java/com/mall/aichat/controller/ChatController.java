package com.mall.aichat.controller;

import com.mall.common.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    // 注入 系统提示词
    @Value("classpath:/prompts/system-prompt.md")
    private org.springframework.core.io.Resource systemPromptResource;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String question, @RequestParam(required = false) String conversationId) {

        SystemMessage systemMessage = new SystemMessage(systemPromptResource);
        UserMessage userMessage = new UserMessage(question);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return qwenChatClient.prompt(prompt)
            .advisors(a -> {
                a.param(ChatMemory.CONVERSATION_ID, conversationId);
                a.param("userId", SecurityUtils.getUserId());
            })
            .stream()
            .content();
    }
}
