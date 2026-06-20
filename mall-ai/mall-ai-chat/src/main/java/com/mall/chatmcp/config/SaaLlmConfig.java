package com.mall.chatmcp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaaLlmConfig {

    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model).build();
    }
}
