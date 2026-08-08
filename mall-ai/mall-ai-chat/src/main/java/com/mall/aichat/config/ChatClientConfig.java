package com.mall.aichat.config;

import com.mall.aichat.advisor.FullHistoryChatMemoryAdvisor;
import com.mall.aichat.advisor.ReturnDirectChatMemoryAdvisor;
import com.mall.aichat.advisor.WrappedMcpToolCallbackProvider;
import com.mall.aichat.service.ISysChatHistoryService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.index.lucene.LuceneToolIndex;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天客户端配置
 */
@Configuration
public class ChatClientConfig {

    @Value("classpath:/prompts/system-prompt-simplify.md")
    private Resource systemSimplifyPromptResource;

    @Value("${vectorstore.chat-memory-default-topk}")
    private int vectorStoreChatMemoryDefaultTopK;

    @Value("${mineru.vl.base-url}")
    private String llmBaseUrl;

    @Value("${mineru.vl.api-key}")
    private String apiKey;

    @Value("${mineru.vl.model}")
    private String model;

    @Value("${vectorstore.enabled}")
    private boolean vectorStoreEnabled;

    @Value("${spring.ai.mcp.client.enabled}")
    private boolean mcpEnabled;

    /**
     * 方案 A: 向量检索索引
     * 当配置 vector.enabled = true 时生效
     */
    @Bean("toolIndex") // 【关键】统一 Bean 名称，这样调用方不需要改
    @ConditionalOnProperty(name = "vectorstore.enabled", havingValue = "true")
    public ToolIndex vectorToolIndex(@Qualifier("toolVectorStore") VectorStore toolVectorStore) {
        return new VectorToolIndex(toolVectorStore);
    }

    /**
     * 方案 B: Lucene 本地索引
     * 当配置 vector.enabled = false (或者配置不存在) 时生效
     * matchIfMissing = true 表示如果没有配置该属性，默认使用 Lucene
     */
    @Bean("toolIndex") // 【关键】统一 Bean 名称
    @ConditionalOnProperty(name = "vectorstore.enabled", havingValue = "false", matchIfMissing = true)
    public ToolIndex toolIndex() {
        return new LuceneToolIndex(0.3f);
    }

    @Bean
    public ToolSearchToolCallingAdvisor toolSearchAdvisor(@Qualifier("toolIndex") ToolIndex toolIndex) {
        return ToolSearchToolCallingAdvisor.builder()
            .toolIndex(toolIndex)
            .build();
    }

    /**
     * 总聊天会话
     *
     * @param model
     * @param chatMemory
     * @param conversationVectorStore
     * @return
     */
    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(OpenAiChatModel model, ChatMemory chatMemory,
                                     @Qualifier("conversationVectorStore") @Autowired(required = false) VectorStore conversationVectorStore,
                                     ToolSearchToolCallingAdvisor toolSearchAdvisor,
                                     ISysChatHistoryService sysChatHistoryService,
                                     StringRedisTemplate mallRedisTemplate,
                                     AgentEventSinkManager agentEventSinkManager,
                                     @Qualifier("mcpAsyncToolCallbacks") @Autowired(required = false) AsyncMcpToolCallbackProvider tools
    ) {
        List<Advisor> advisors = new ArrayList<>();

        // 1. 基础内存（Redis/MySQL）
        advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).order(Ordered.HIGHEST_PRECEDENCE + 199).build());

        // 2. 向量内存 - 根据 vectorEnabled 和 conversationVectorStore 是否存在来决定
        if (vectorStoreEnabled && conversationVectorStore != null) {
            advisors.add(VectorStoreChatMemoryAdvisor.builder(conversationVectorStore)
                .order(2)
                .defaultTopK(vectorStoreChatMemoryDefaultTopK)
                .build());
        }

        //保存全量消息
        advisors.add(FullHistoryChatMemoryAdvisor.builder(sysChatHistoryService, mallRedisTemplate, chatMemory, agentEventSinkManager).order(3).build());

        //存储标记了returnDirect=true的工具结果
        advisors.add(ReturnDirectChatMemoryAdvisor.builder(sysChatHistoryService, mallRedisTemplate, chatMemory).order(Ordered.HIGHEST_PRECEDENCE + 200).build());

        // 4. 日志顾问
        advisors.add(new SimpleLoggerAdvisor(4));

        // 工具搜索顾问 - 如果 ToolIndex 依赖向量库，这里也需要做判断
        advisors.add(toolSearchAdvisor);

        ChatClient.Builder builder = ChatClient.builder(model)
            .defaultSystem(systemSimplifyPromptResource)
            .defaultAdvisors(advisors);

        if (mcpEnabled) {
            WrappedMcpToolCallbackProvider wrappedMcpToolCallbackProvider = new WrappedMcpToolCallbackProvider(tools);
            builder.defaultTools(wrappedMcpToolCallbackProvider);
        }
        return builder.build();
    }

    /**
     * 会话标题概述会话
     *
     * @param model
     * @return
     */
    @Bean(name = "titleChatClient")
    public ChatClient titleChatClient(OpenAiChatModel model) {
        return ChatClient
            .builder(model)
            .defaultSystem("""
                你是会话标题助手。任务：在用户发送首个问题时，用一句简短、明确的概述该问题，
                用作会话标题。要求：不超过20字，仅输出概述，不带任何解释或标点。禁止输出“标题是”“概述”“会话标题”等前缀或说明。
                """)
            .build();
    }

    /**
     * 向量压缩会话
     *
     * @param model
     * @return
     */
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

    /**
     * ocr图片解析会话
     *
     * @return
     */
    @Bean("minerUChatClient")
    public ChatClient minerUChatClient() {
        // 1) 构造同步客户端
        OpenAIClient client = OpenAIOkHttpClient.builder()
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
