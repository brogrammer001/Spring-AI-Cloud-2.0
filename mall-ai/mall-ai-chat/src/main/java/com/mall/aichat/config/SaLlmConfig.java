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
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Value("${vector-store.chat-memory-default-topk}")
    private int vectorStoreChatMemoryDefaultTopK;

    @Value("${chat-memory.max-messages}")
    private int chatMemoryMaxMessages;


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
            .maxMessages(chatMemoryMaxMessages) //达到4条时，会直接删除最老的2条对话，偶数 ，向下取整
            .chatMemoryRepository(chatMemoryRepository)
            .build();
        return new FullHistoryChatMemory(memory, sysChatHistoryService, mallRedisTemplate);
    }


    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(OpenAiChatModel model, ChatMemory chatMemory,
                                     IAiConversationService aiConversationService,
                                     @Qualifier("conversationVectorStore")VectorStore conversationVectorStore) {
        return ChatClient
            .builder(model)
            .defaultSystem(systemPromptResource)
            .defaultAdvisors(
                new ConversationInitAdvisor(aiConversationService),
                MessageChatMemoryAdvisor.builder(chatMemory).order(1).build(),
                VectorStoreChatMemoryAdvisor.builder(conversationVectorStore).order(2).defaultTopK(vectorStoreChatMemoryDefaultTopK).build(),
                new SimpleLoggerAdvisor(3)
            )
            .build();
    }
}
