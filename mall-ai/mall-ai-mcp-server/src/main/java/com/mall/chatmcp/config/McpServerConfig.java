package com.mall.chatmcp.config;

import com.mall.chatmcp.sevice.BaseToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider tools(List<BaseToolService> allTools)
    {
        return MethodToolCallbackProvider.builder()
            .toolObjects(allTools)
            .build();
    }
}
