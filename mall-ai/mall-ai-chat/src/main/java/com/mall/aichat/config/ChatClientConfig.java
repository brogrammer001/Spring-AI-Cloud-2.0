package com.mall.aichat.config;

import com.mall.aichat.advisor.WrappedMcpToolCallbackProvider;
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
import org.springframework.core.io.Resource;

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

    /**
     * 方案 A: 向量检索索引
     * 当配置 vector.enabled = true 时生效
     */
    @Bean("toolIndex") // 【关键】统一 Bean 名称，这样调用方不需要改
    @ConditionalOnProperty(name = "vector.enabled", havingValue = "true")
    public ToolIndex vectorToolIndex(@Qualifier("toolVectorStore") VectorStore toolVectorStore) {
        return new VectorToolIndex(toolVectorStore);
    }

    /**
     * 方案 B: Lucene 本地索引
     * 当配置 vector.enabled = false (或者配置不存在) 时生效
     * matchIfMissing = true 表示如果没有配置该属性，默认使用 Lucene
     */
    @Bean("toolIndex") // 【关键】统一 Bean 名称
    @ConditionalOnProperty(name = "vector.enabled", havingValue = "false", matchIfMissing = true)
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
                                     @Qualifier("mcpAsyncToolCallbacks") AsyncMcpToolCallbackProvider tools
    ) {
        List<Advisor> advisors = new ArrayList<>();

        // 1. 基础内存（Redis/MySQL）
        advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).order(1).build());

        // 2. 向量内存 - 根据 vectorEnabled 和 conversationVectorStore 是否存在来决定
        if (vectorStoreEnabled && conversationVectorStore != null) {
            advisors.add(VectorStoreChatMemoryAdvisor.builder(conversationVectorStore)
                .order(2)
                .defaultTopK(vectorStoreChatMemoryDefaultTopK)
                .build());
        }

        // 3. 工具搜索顾问 - 如果 ToolIndex 依赖向量库，这里也需要做判断
        advisors.add(toolSearchAdvisor);

        // 4. 日志顾问
        advisors.add(new SimpleLoggerAdvisor(3));

        WrappedMcpToolCallbackProvider wrappedMcpToolCallbackProvider = new WrappedMcpToolCallbackProvider(tools);

        return ChatClient.builder(model)
            .defaultSystem(systemSimplifyPromptResource)
            .defaultTools(wrappedMcpToolCallbackProvider)
            .defaultAdvisors(advisors) // 传入动态构建的列表
            .build();
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
     * 文档解析后内容再次解析会话
     *
     * @param model
     * @return
     */
    @Bean(name = "reparsingChatClient")
    public ChatClient reparsingChatClient(OpenAiChatModel model) {
        return ChatClient
            .builder(model)
            .defaultSystem("""
                 你是一个专业的文档数据清洗与质量审核专家。你的任务是处理由 MinerU 等 PDF 解析工具输出的 Markdown 文本，进行清洗、验证与重组。
                # 核心任务与规则
                ## 1. 去除死循环与解析标签（强制执行）
                - 必须彻底清除 MinerU 解析异常产生的死循环标签，例如连续重复的 `<|txt_contd_tgt|>`, `<|txt_contd_src|>`。
                - 清除所有类似 `<|xxx|>` 格式的系统残留标签。
                ## 2. 语言一致性校验（重点）
                针对“图片解析出英文内容”的情况，请按以下逻辑判断保留还是清洗：
                - **场景 A：原文确实是英文**
                  如果文档主体是中文，但该英文片段属于：专业术语、公式变量、代码片段、参考文献、图表英文标题，且语义通顺，**请予以保留**。
                - **场景 B：解析错误/模型幻觉**
                  如果文档主体是中文，出现的英文片段符合以下特征，**请判定为异常并尝试修复或删除**：
                  1. **语义断裂**：该英文无法与上下文连接，显得突兀。
                  2. **描述性幻觉**：内容看起来像是多模态模型对图片的英文描述（如 "Figure shows a graph of..."），而非文档正文内容。
                  3. **OCR乱码**：英文单词拼写错误严重，或混杂了无意义的符号。
                  *处理方式：若判断为解析错误，请直接删除该片段，或标注 `[图片解析异常]`。*
                ## 3. 格式修复与重组
                - 修复断裂的段落。
                - 修复 Markdown 表格格式。
                - 去除页眉页脚、页码、乱码行。
                ## 4. 输出要求
                - 直接输出清洗后的 Markdown 内容。
                - 若遇到无法修复的严重乱码，输出 `[CONTENT_ERROR: 解析失败]`。
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
