package com.mall.aichat.config;

import com.mall.aichat.advisor.ConversationInitAdvisor;
import com.mall.aichat.advisor.FullHistoryChatMemory;
import com.mall.aichat.advisor.RedisCachedAndMysqlMemoryRepository;
import com.mall.aichat.service.IAiConversationService;
import com.mall.aichat.service.ISysChatHistoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class SaLlmConfig {

    // 注入 系统提示词
    @Value("classpath:/prompts/system-prompt.md")
    private Resource systemPromptResource;

    /**
     * 配置向量存储会话 advisor
     * @param vectorStore
     * @return
     */
    @Bean
    public VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor(VectorStore vectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
            .order(200)      // 执行顺序在 MessageChatMemoryAdvisor 之后
            .defaultTopK(1)
            .build();
    }


    /**
     * 会话记忆存储
     * @param jdbcChatMemoryRepository
     * @param mallRedisTemplate
     * @return
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository, ISysChatHistoryService sysChatHistoryService, StringRedisTemplate mallRedisTemplate){
        ChatMemoryRepository chatMemoryRepository = new RedisCachedAndMysqlMemoryRepository(jdbcChatMemoryRepository, mallRedisTemplate);
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(4) //达到4条时，会直接删除最老的2条对话，偶数 ，向下取整
            .chatMemoryRepository(chatMemoryRepository)
            .build();
        return new FullHistoryChatMemory(memory, sysChatHistoryService, mallRedisTemplate);
    }


    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(OpenAiChatModel model, ChatMemory chatMemory,
                                     IAiConversationService aiConversationService,
                                     VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor) {
        return ChatClient
            .builder(model)
            .defaultSystem(systemPromptResource)
            .defaultAdvisors(
                new ConversationInitAdvisor(aiConversationService),
                MessageChatMemoryAdvisor.builder(chatMemory).order(1).build(),
                vectorStoreChatMemoryAdvisor,
                new SimpleLoggerAdvisor()
            )
            .build();
    }
}
