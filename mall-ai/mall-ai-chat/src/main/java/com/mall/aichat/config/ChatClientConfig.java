package com.mall.aichat.config;

import com.mall.aichat.advisor.*;
import com.mall.aichat.service.IAiAgentToolCallLogService;
import com.mall.aichat.service.IKbDocumentService;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.aichat.service.impl.RerankerService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.ai.session.compaction.TurnCountTrigger;
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
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天客户端配置
 */
@Configuration
public class ChatClientConfig {

    @Value("${vectorstore.chat-memory-default-topk}")
    private int vectorStoreChatMemoryDefaultTopK;

    @Value("${mineru.vl.base-url}")
    private String llmBaseUrl;

    @Value("${mineru.vl.api-key}")
    private String apiKey;

    @Value("${mineru.vl.model}")
    private String model;

    @Value("${smallmodel.base-url}")
    private String smallModelBaseUrl;

    @Value("${smallmodel.api-key}")
    private String smallModelApiKey;

    @Value("${smallmodel.model}")
    private String smallModelName;

    @Value("${vectorstore.enabled}")
    private boolean vectorStoreEnabled;

    @Value("${spring.ai.mcp.client.enabled}")
    private boolean mcpEnabled;

    @Value("classpath:/prompts/system-prompt-simplify.md")
    private org.springframework.core.io.Resource systemSimplifyPromptResource;

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
            .advisorOrder(100)
            .build();
    }

    @Bean
    public SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService) {
        return SessionMemoryAdvisor.builder(sessionService)
            .compactionTrigger(new TurnCountTrigger(4))
            .order(101)
            .compactionStrategy(
                SlidingWindowCompactionStrategy.builder()
                    .maxEvents(8)
                    .build())
            .build();
    }

    /**
     * 总聊天会话
     *
     * @param model
     * @param conversationVectorStore
     * @return
     */
    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(OpenAiChatModel model, SessionMemoryAdvisor sessionMemoryAdvisor,
                                     @Qualifier("conversationVectorStore") @Autowired(required = false) VectorStore conversationVectorStore,
                                     @Qualifier("knowledgeVectorStore") @Autowired(required = false) VectorStore knowledgeVectorStore,
                                     ToolSearchToolCallingAdvisor toolSearchAdvisor,
                                     ISysChatHistoryService sysChatHistoryService,
                                     IKbDocumentService kbDocumentService,
                                     RerankerService rerankerService,
                                     StringRedisTemplate mallRedisTemplate,
                                     AgentEventSinkManager agentEventSinkManager,
                                     SessionService sessionService,
                                     ChatClient smallChatClient,
                                     IAiAgentToolCallLogService toolCallLogService,
                                     @Qualifier("mcpAsyncToolCallbacks") @Autowired(required = false) AsyncMcpToolCallbackProvider tools
    ) {
        List<Advisor> advisors = new ArrayList<>();

        // 1. 向量会话记忆 - 根据 vectorEnabled 和 conversationVectorStore 是否存在来决定
        if (vectorStoreEnabled && conversationVectorStore != null) {
            advisors.add(VectorStoreChatMemoryAdvisor.builder(conversationVectorStore, smallChatClient)
                .order(98)
                .defaultTopK(vectorStoreChatMemoryDefaultTopK)
                .build());
        }

        // 2. 在 toolSearchAdvisor 之前执行，保存标记了 returnDirect=true 的工具结果
        advisors.add(ReturnDirectChatMemoryAdvisor.builder(sysChatHistoryService, mallRedisTemplate, sessionService).order(99).build());

        // 3.工具搜索顾问
        advisors.add(toolSearchAdvisor); //100

        // 4.基础会话记忆（MySQL）
        advisors.add(sessionMemoryAdvisor); // 101

        // 5. 知识库上下文查询 - 根据 vectorEnabled 和 knowledgeVectorStore 是否存在来决定
        if (vectorStoreEnabled && knowledgeVectorStore != null) {
            advisors.add(RagContextQueryAdvisor.builder(knowledgeVectorStore, kbDocumentService, rerankerService, agentEventSinkManager)
                .order(102)
                .build());
        }

        // 6. 保存全量消息，必须在 toolSearchAdvisor 之后，要拿到工具调用信息
        advisors.add(HistoryChatMemoryAdvisor.builder(sysChatHistoryService, mallRedisTemplate, agentEventSinkManager, toolCallLogService).order(103).build());

        // 7. 观测日志
        advisors.add(new SimpleLoggerAdvisor(104));

        ChatClient.Builder builder = ChatClient.builder(model)
            .defaultSystem(systemSimplifyPromptResource)
            .defaultAdvisors(advisors);

        if (mcpEnabled) {
            // ★ 双层装饰器链：ReturnDirect 包装 → 审计日志包装
            WrappedMcpToolCallbackProvider wrappedProvider = new WrappedMcpToolCallbackProvider(tools);
            builder.defaultTools(wrappedProvider);
        }
        return builder.build();
    }

    /**
     * 概述小模型，专门用于概述总结
     *
     * @return
     */
    @Bean("smallChatClient")
    public ChatClient smallChatClient() {
        // 1) 构造同步客户端
        OpenAIClient client = OpenAIOkHttpClient.builder()
            .baseUrl(smallModelBaseUrl)
            .apiKey(smallModelApiKey)
            .timeout(Duration.ofMinutes(5)) // 设置超时时间为 5 分钟
            .build();

        // 2) 选项中补全 apiKey 和 baseUrl，供 build() 内部创建 async client 使用
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(smallModelName)
            .apiKey(smallModelApiKey)     // 关键：补上 apiKey
            .baseUrl(smallModelBaseUrl)     // 关键：补上 baseUrl
            .temperature(0.0)
            .maxTokens(4096)
            .build();

        // 3) 构造 OpenAiChatModel
        OpenAiChatModel smallChatModel = OpenAiChatModel.builder()
            .openAiClient(client)
            .options(options)
            .build();

        // 4) 包装成 ChatClient
        return ChatClient.builder(smallChatModel).build();
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
            .temperature(0.1)
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
