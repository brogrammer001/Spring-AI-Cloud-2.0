package com.mall.aichat.config;

import com.mall.aichat.advisor.ConversationInitAdvisor;
import com.mall.aichat.service.IAiConversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class SaLlmConfig {

    @Bean
    public VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor(VectorStore vectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
            .order(200)      // 执行顺序在 MessageChatMemoryAdvisor 之后
            .defaultTopK(5)         // 每次最多召回 5 条历史片段
            .build();
    }


    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository, StringRedisTemplate mallRedisTemplate){
        ChatMemoryRepository chatMemoryRepository = new RedisCachedMemoryRepository(jdbcChatMemoryRepository, mallRedisTemplate);
        return MessageWindowChatMemory.builder()
            .maxMessages(3)
            .chatMemoryRepository(chatMemoryRepository)
            .build();
    }


    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(OpenAiChatModel model, ChatMemory chatMemory, IAiConversationService aiConversationService, VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor) {
        // 1. 使用 PromptTemplate 加载 md 文件
        //PromptTemplate systemPromptTemplate = new PromptTemplate(systemPromptResource);
        // 如果没有变量，直接 render
        //String systemContent = systemPromptTemplate.render();
        return ChatClient
            .builder(model)
            //.defaultSystem(systemContent)
            .defaultAdvisors(
                new ConversationInitAdvisor(aiConversationService),
                MessageChatMemoryAdvisor.builder(chatMemory).order(1).build(),
                vectorStoreChatMemoryAdvisor
            )
            .build();
    }
}
