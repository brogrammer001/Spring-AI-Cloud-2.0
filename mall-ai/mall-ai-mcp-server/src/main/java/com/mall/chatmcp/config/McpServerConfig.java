package com.mall.chatmcp.config;

import com.mall.chatmcp.sevice.ChatMcpService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider tools(ChatMcpService chatMcpService)
    {
        return MethodToolCallbackProvider.builder()
            .toolObjects(chatMcpService)
            .build();
    }
}
