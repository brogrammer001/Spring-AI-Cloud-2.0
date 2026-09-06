package com.mall.aichat.advisor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.ToolSearchTool;
import org.springframework.ai.tool.toolsearch.eviction.LruEvictionStrategy;
import org.springframework.ai.tool.toolsearch.eviction.ToolIndexEvictionStrategy;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ToolSearchToolCallingAdvisor} 的增强子类，修复"渐进式工具披露"在跨轮/压缩/多迭代场景下
 * 丢失工具回调导致的 {@code No ToolCallback found for tool name} 问题。
 *
 * <p>父类 {@code prepareIteration} 在<b>每一轮迭代</b>都会用重新计算的 {@code selectedToolCallbacks}
 * 整体覆盖 {@code options.toolCallbacks}，而该集合只包含 {@code toolSearchTool} + 当前消息窗口内
 * {@code toolSearchTool} 响应里发现的工具。当会话历史被压缩或跨轮请求时，最初的搜索发现响应可能已不在
 * 窗口内，但 LLM 仍会从持久化历史里看到自己之前调用过某工具，于是直接复调——此时回调集合里没有它，执行即失败。
 *
 * <p>本类采用<b>双层防护</b>：
 * <ol>
 *   <li><b>Prompt 侧</b>（{@link #restoreHistoryReferencedTools}）：每轮 doBeforeCall/doBeforeStream 在
 *       父类覆盖之后，把历史 Assistant 消息里调用过的工具定义补回，保证模型能合法地看到并复调；</li>
 *   <li><b>执行侧</b>（{@link HistoryAwareToolCallingManager}）：装饰传给父类的 ToolCallingManager，
 *       无论父类内部循环覆盖多少次，真正执行工具前都会依据 sessionId 从全量注册表兜底补全回调。</li>
 * </ol>
 * 两层配合下，prompt 里默认仍只暴露已发现工具（保留 token 优势），而执行侧永远不缺 callback。
 */
public class HistoryAwareToolSearchAdvisor extends ToolSearchToolCallingAdvisor {

    /**
     * 上下文 key：保存本轮会话完整的原始工具回调（name -> callback）。
     */
    private static final String FULL_TOOL_CALLBACKS_KEY = HistoryAwareToolSearchAdvisor.class.getName()
        + ".fullToolCallbacks";

    /**
     * 装饰后的 ToolCallingManager，供执行期兜底补全，并持有会话级全量工具注册表。
     */
    private final HistoryAwareToolCallingManager decoratedManager;

    /**
     * 对外构造入口：自动把传入的 {@link ToolCallingManager} 包装成 {@link HistoryAwareToolCallingManager}
     * 后再交给父类，确保父类内部循环用的就是装饰后的实例。
     */
    protected HistoryAwareToolSearchAdvisor(ToolCallingManager toolCallingManager, int advisorOrder,
                                            ToolExecutionEligibilityChecker toolExecutionEligibilityChecker,
                                            ToolIndex toolIndex, String systemMessageSuffix,
                                            boolean referenceToolNameAccumulation, Integer maxResults,
                                            boolean conversationHistoryEnabled, String sessionIdKeyName,
                                            ToolIndexEvictionStrategy evictionStrategy) {
        this(new HistoryAwareToolCallingManager(toolCallingManager), advisorOrder, toolExecutionEligibilityChecker,
            toolIndex, systemMessageSuffix, referenceToolNameAccumulation, maxResults, conversationHistoryEnabled,
            sessionIdKeyName, evictionStrategy);
    }

    private HistoryAwareToolSearchAdvisor(HistoryAwareToolCallingManager decoratedManager, int advisorOrder,
                                          ToolExecutionEligibilityChecker toolExecutionEligibilityChecker,
                                          ToolIndex toolIndex, String systemMessageSuffix,
                                          boolean referenceToolNameAccumulation, Integer maxResults,
                                          boolean conversationHistoryEnabled, String sessionIdKeyName,
                                          ToolIndexEvictionStrategy evictionStrategy) {
        super(decoratedManager, advisorOrder, toolExecutionEligibilityChecker, toolIndex, systemMessageSuffix,
            referenceToolNameAccumulation, maxResults, conversationHistoryEnabled, sessionIdKeyName,
            evictionStrategy);
        this.decoratedManager = decoratedManager;
    }

    @Override
    public String getName() {
        return "HistoryAwareToolSearchAdvisor";
    }

    // ------------------------------------------------------------------
    // 同步钩子
    // ------------------------------------------------------------------

    @Override
    protected ChatClientRequest doBeforeCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        // 先捕获原始全量（此时父类尚未覆盖 toolCallbacks），再走父类披露逻辑，最后补回历史工具
        captureFullToolCallbacks(chatClientRequest);
        return restoreHistoryReferencedTools(super.doBeforeCall(chatClientRequest, chain));
    }

    // ------------------------------------------------------------------
    // 流式钩子
    // ------------------------------------------------------------------

    @Override
    protected ChatClientRequest doBeforeStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        captureFullToolCallbacks(chatClientRequest);
        return restoreHistoryReferencedTools(super.doBeforeStream(chatClientRequest, chain));
    }

    // ------------------------------------------------------------------
    // 会话清理
    // ------------------------------------------------------------------

    @Override
    public void evictSession(String sessionId) {
        this.decoratedManager.unregister(sessionId);
        super.evictSession(sessionId);
    }

    // ------------------------------------------------------------------
    // 核心逻辑
    // ------------------------------------------------------------------

    /**
     * 从原始 options 捕获全量工具回调，写入 context 并按 sessionId 注册到装饰 manager。
     * 放在每轮必走的 doBeforeCall/doBeforeStream 中，保证新鲜度与覆盖所有请求路径。
     */
    private void captureFullToolCallbacks(ChatClientRequest chatClientRequest) {
        if (!(chatClientRequest.prompt().getOptions() instanceof ToolCallingChatOptions options)) {
            return;
        }
        List<ToolCallback> callbacks = options.getToolCallbacks();
        if (CollectionUtils.isEmpty(callbacks)) {
            return;
        }
        Map<String, ToolCallback> fullToolCallbacks = new ConcurrentHashMap<>();
        callbacks.forEach(tc -> fullToolCallbacks.putIfAbsent(tc.getToolDefinition().name(), tc));
        chatClientRequest.context().put(FULL_TOOL_CALLBACKS_KEY, fullToolCallbacks);

        // sessionId 由父类 initializeSession 写入 context（此时已就绪）
        Object sessionId = chatClientRequest.context().get(ToolSearchTool.TOOL_SEARCH_TOOL_SESSION_ID_KEY);
        if (sessionId != null) {
            this.decoratedManager.register(sessionId.toString(), fullToolCallbacks);
        }
    }

    /**
     * 扫描历史 Assistant 消息里的工具调用，将其中"当前回调集合缺失但原始集合存在"的工具补回，
     * 并显式保留 toolContext（防御 mutate 未透传导致 sessionId 丢失）。
     */
    @SuppressWarnings("unchecked")
    private ChatClientRequest restoreHistoryReferencedTools(ChatClientRequest chatClientRequest) {
        if (!(chatClientRequest.prompt().getOptions() instanceof ToolCallingChatOptions options)) {
            return chatClientRequest;
        }
        Map<String, ToolCallback> fullToolCallbacks =
            (Map<String, ToolCallback>) chatClientRequest.context().get(FULL_TOOL_CALLBACKS_KEY);
        if (CollectionUtils.isEmpty(fullToolCallbacks)) {
            return chatClientRequest;
        }

        List<ToolCallback> currentCallbacks = options.getToolCallbacks();
        Set<String> currentNames = new HashSet<>();
        if (!CollectionUtils.isEmpty(currentCallbacks)) {
            currentCallbacks.forEach(tc -> currentNames.add(tc.getToolDefinition().name()));
        }

        List<ToolCallback> augmented = new ArrayList<>(
            CollectionUtils.isEmpty(currentCallbacks) ? List.of() : currentCallbacks);
        boolean changed = false;
        for (Message message : chatClientRequest.prompt().getInstructions()) {
            if (message instanceof AssistantMessage am && am.hasToolCalls()) {
                for (AssistantMessage.ToolCall toolCall : am.getToolCalls()) {
                    String toolName = toolCall.name();
                    if (StringUtils.hasText(toolName) && !currentNames.contains(toolName)
                        && fullToolCallbacks.containsKey(toolName)) {
                        augmented.add(fullToolCallbacks.get(toolName));
                        currentNames.add(toolName);
                        changed = true;
                    }
                }
            }
        }

        if (!changed) {
            return chatClientRequest;
        }

        ToolCallingChatOptions augmentedOptions = ((ToolCallingChatOptions.Builder<?>) options.mutate())
            .toolCallbacks(augmented)
            .toolContext(options.getToolContext())
            .build();

        return chatClientRequest.mutate()
            .prompt(chatClientRequest.prompt().mutate().chatOptions(augmentedOptions).build())
            .build();
    }

    // ------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 构建 {@link HistoryAwareToolSearchAdvisor}。除 toolIndex / advisorOrder 外的默认值
     * 与父类 {@code ToolSearchToolCallingAdvisor.Builder} 保持一致。
     */
    public static final class Builder {

        private ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        private ToolExecutionEligibilityChecker toolExecutionEligibilityChecker =
            chatResponse -> chatResponse != null && chatResponse.hasToolCalls();

        private int advisorOrder = DEFAULT_ORDER;

        private ToolIndex toolIndex;

        private String systemMessageSuffix;

        private boolean referenceToolNameAccumulation = true;

        private Integer maxResults;

        private boolean conversationHistoryEnabled = true;

        private String sessionIdKeyName = ChatMemory.CONVERSATION_ID;

        private ToolIndexEvictionStrategy evictionStrategy = new LruEvictionStrategy(1000);

        private Builder() {
        }

        public Builder toolIndex(ToolIndex toolIndex) {
            Assert.notNull(toolIndex, "toolIndex cannot be null");
            this.toolIndex = toolIndex;
            return this;
        }

        public Builder advisorOrder(int advisorOrder) {
            this.advisorOrder = advisorOrder;
            return this;
        }

        public Builder toolCallingManager(ToolCallingManager toolCallingManager) {
            Assert.notNull(toolCallingManager, "toolCallingManager cannot be null");
            this.toolCallingManager = toolCallingManager;
            return this;
        }

        public Builder toolExecutionEligibilityChecker(ToolExecutionEligibilityChecker toolExecutionEligibilityChecker) {
            Assert.notNull(toolExecutionEligibilityChecker, "toolExecutionEligibilityChecker cannot be null");
            this.toolExecutionEligibilityChecker = toolExecutionEligibilityChecker;
            return this;
        }

        public Builder systemMessageSuffix(String systemMessageSuffix) {
            Assert.hasText(systemMessageSuffix, "systemMessageSuffix cannot be null or empty");
            this.systemMessageSuffix = systemMessageSuffix;
            return this;
        }

        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder referenceToolNameAccumulation(boolean referenceToolNameAccumulation) {
            this.referenceToolNameAccumulation = referenceToolNameAccumulation;
            return this;
        }

        public Builder conversationHistoryEnabled(boolean conversationHistoryEnabled) {
            this.conversationHistoryEnabled = conversationHistoryEnabled;
            return this;
        }

        public Builder sessionIdKeyName(String sessionIdKeyName) {
            Assert.hasText(sessionIdKeyName, "sessionIdKeyName cannot be null or empty");
            this.sessionIdKeyName = sessionIdKeyName;
            return this;
        }

        public Builder evictionStrategy(ToolIndexEvictionStrategy evictionStrategy) {
            Assert.notNull(evictionStrategy, "evictionStrategy must not be null");
            this.evictionStrategy = evictionStrategy;
            return this;
        }

        public HistoryAwareToolSearchAdvisor build() {
            Assert.notNull(this.toolIndex, "toolIndex is required");
            String suffix = this.systemMessageSuffix;
            if (!StringUtils.hasText(suffix)) {
                try {
                    suffix = new DefaultResourceLoader()
                        .getResource("classpath:/DEFAULT_SYSTEM_PROMPT_SUFFIX.md")
                        .getContentAsString(StandardCharsets.UTF_8);
                }
                catch (Exception ex) {
                    throw new IllegalArgumentException(
                        "Failed to load default system message suffix from classpath resource", ex);
                }
            }
            return new HistoryAwareToolSearchAdvisor(this.toolCallingManager, this.advisorOrder,
                this.toolExecutionEligibilityChecker, this.toolIndex, suffix, this.referenceToolNameAccumulation,
                this.maxResults, this.conversationHistoryEnabled, this.sessionIdKeyName, this.evictionStrategy);
        }
    }
}
