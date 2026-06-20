package com.mall.chatmcp.config;

import com.mall.chatmcp.advisor.RequestAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class SaaLlmConfig {


    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(OpenAiChatModel model) {
        // 1. 使用 PromptTemplate 加载 md 文件
        //PromptTemplate systemPromptTemplate = new PromptTemplate(systemPromptResource);
        // 如果没有变量，直接 render
        //String systemContent = systemPromptTemplate.render();

        return ChatClient
            .builder(model)
            //.defaultSystem(systemContent)
            .defaultAdvisors(new RequestAdvisor())
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .build();
    }
}
