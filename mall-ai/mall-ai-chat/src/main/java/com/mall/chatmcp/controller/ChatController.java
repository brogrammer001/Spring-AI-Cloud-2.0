package com.mall.chatmcp.controller;

import com.mall.chatmcp.config.RagConfig;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;

    // 注入 系统提示词
    @Value("classpath:/prompts/system-prompt.md")
    private org.springframework.core.io.Resource systemPromptResource;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String question, @RequestParam String conversationId) {

        SystemMessage systemMessage = new SystemMessage(systemPromptResource);
        UserMessage userMessage = new UserMessage(question);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return qwenChatClient.prompt(prompt)
            .stream()
            .content();
    }

    @GetMapping(value = "/send", produces = "text/plain;charset=UTF-8")
    public Flux<String> send(@RequestParam String question) {
        SystemMessage systemMessage = new SystemMessage(systemPromptResource);
        UserMessage userMessage = new UserMessage(question);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        return qwenChatClient.prompt(prompt)
            .stream()
            .content();
    }
}
