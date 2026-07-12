package com.mall.aichat.config;

import com.mall.aichat.advisor.RedisCachedAndMysqlMemoryRepository;
import com.mall.aichat.service.ISysChatHistoryService;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class SaLlmConfig {

    // 注入 系统提示词
    @Value("classpath:/prompts/system-prompt.md")
    private Resource systemPromptResource;

    @Value("${vector-store.chat-memory-default-topk}")
    private int vectorStoreChatMemoryDefaultTopK;

    @Value("${chat-memory.max-messages}")
    private int chatMemoryMaxMessages;

    @Value("${mineru.vl.base-url}")
    private String llmBaseUrl;

    @Value("${mineru.vl.api-key}")
    private String apiKey;

    @Value("${mineru.vl.model}")
    private String model;

    @Value("${mineru.base-url}")
    private String baseUrl;

    @Value("${mineru.token}")
    private String token;

    @Bean
    public RestClient mineruRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
            .requestFactory(factory)
            .build();
    }

    /**
     * 会话记忆存储
     *
     * @param jdbcChatMemoryRepository
     * @param mallRedisTemplate
     * @return
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository, ISysChatHistoryService sysChatHistoryService, StringRedisTemplate mallRedisTemplate) {
        ChatMemoryRepository chatMemoryRepository = new RedisCachedAndMysqlMemoryRepository(jdbcChatMemoryRepository, mallRedisTemplate, sysChatHistoryService); //redis/mysql存储最近几轮对话
        return MessageWindowChatMemory.builder()
            .maxMessages(chatMemoryMaxMessages) //达到4条时，会直接删除最老的2条对话，偶数，向下取整
            .chatMemoryRepository(chatMemoryRepository)
            .build();
    }


    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(OpenAiChatModel model, ChatMemory chatMemory,
                                     @Qualifier("conversationVectorStore") VectorStore conversationVectorStore) {
        return ChatClient
            .builder(model)
            .defaultSystem(systemPromptResource)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).order(1).build(), //redis/mysql存储会话记忆
                VectorStoreChatMemoryAdvisor.builder(conversationVectorStore).order(2).defaultTopK(vectorStoreChatMemoryDefaultTopK).build(), //向量库存储全量会话
                new SimpleLoggerAdvisor(3)
            )
            .build();
    }

    @Bean(name = "compressChatClient")
    public ChatClient compressChatClient(OpenAiChatModel model) {
        return ChatClient
            .builder(model)
            .defaultSystem("""
                请从以下对话中提取关键的用户偏好、事实和决策，输出为JSON列表格式：
                [
                  {"fact": "用户不喜欢吃香菜"},
                  {"fact": "用户正在学习 Spring AI"}
                ]
                对话记录：
                %s
                """)
            .build();
    }

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 2. 核心线程数：线程池创建时候初始化的线程数
        // 建议设置为 CPU 核心数 + 1，或者根据你的并发量调整（例如 5）
        executor.setCorePoolSize(5);

        // 3. 最大线程数：线程池最大的线程数，只有在队列满了之后才会创建超过核心线程数的线程
        // 压缩任务比较耗时（调用LLM），建议稍微大一点
        executor.setMaxPoolSize(10);

        // 4. 队列容量：用来缓冲执行任务的队列
        // 如果并发量大，队列可以设大一点，比如 100-200
        executor.setQueueCapacity(100);

        // 5. 线程名称前缀：方便在日志中排查问题
        executor.setThreadNamePrefix("async-compress-");

        // 6. 拒绝策略：当队列满了且线程数达到最大值时的处理策略
        // CallerRunsPolicy：由调用者所在的线程来执行任务（即不进线程池，在主线程跑）。
        // 这样可以防止任务丢失，但会稍微阻塞主线程。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 7. 等待所有任务结束后再关闭线程池（保证应用关闭时数据不丢失）
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    @Bean("minerUChatClient")
    public ChatClient minerUChatClient() {
        // 1) 构造同步客户端
        com.openai.client.OpenAIClient client = OpenAIOkHttpClient.builder()
            .baseUrl(llmBaseUrl)
            .apiKey(apiKey)
            .timeout(Duration.ofMinutes(5)) // 设置超时时间为 5 分钟
            .build();

        // 2) 选项中补全 apiKey 和 baseUrl，供 build() 内部创建 async client 使用
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(model)
            .apiKey(apiKey)     // 关键：补上 apiKey
            .baseUrl(llmBaseUrl)     // 关键：补上 baseUrl
            .temperature(0.2)
            .maxTokens(4096) // 增加最大 token 限制，但要有上限防止死循环
            .build();

        // 3) 构造 OpenAiChatModel
        OpenAiChatModel minerUModel = OpenAiChatModel.builder()
            .openAiClient(client)
            .options(options)
            .build();

        // 4) 包装成 ChatClient
        return ChatClient.builder(minerUModel).build();
    }
}
