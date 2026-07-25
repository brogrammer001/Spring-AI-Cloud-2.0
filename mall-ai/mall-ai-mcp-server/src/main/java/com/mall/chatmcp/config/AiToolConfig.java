package com.mall.chatmcp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class AiToolConfig {

    @Bean
    @Primary
    public ChatClient chatClient(OpenAiChatModel model) {
        var toolSearchAdvisor = ToolSearchToolCallingAdvisor.builder()
                .toolIndex(new RegexToolIndex())
                .maxResults(5)
                .build();

        return ChatClient.builder(model)
                .defaultAdvisors(List.of(toolSearchAdvisor))
                .build();
    }

}