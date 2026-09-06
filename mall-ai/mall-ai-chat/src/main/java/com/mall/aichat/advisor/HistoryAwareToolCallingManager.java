package com.mall.aichat.advisor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.toolsearch.ToolSearchTool;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link ToolCallingManager} 的执行期兜底装饰器。
 *
 * <p>父类 {@code ToolSearchToolCallingAdvisor} 在 tool-calling 循环的<b>每一轮迭代</b>都会用
 * 重新计算的 {@code selectedToolCallbacks} 整体覆盖 {@code options.toolCallbacks}。因此仅在
 * Advisor 的 {@code doBeforeCall} 里补回工具，只能保证"第 1 次 LLM 调用前"可解析；进入第 2 轮
 * 及以后的迭代后，若某个历史工具不在本次搜索结果里，仍会被父类丢弃，导致执行时再次报
 * {@code No ToolCallback found}。
 *
 * <p>本装饰器在真正 {@link #executeToolCalls} 之前，依据父类写入 toolContext 的 sessionId，从
 * 会话级注册表里取回<b>全量工具回调</b>，把当前 options 中缺失的补回去，从而保证无论父类内部
 * 覆盖多少次，执行侧永远能解析到对应的 {@link ToolCallback}。
 *
 * <p>注意：这里只补"执行侧"的回调解析；模型 prompt 里能看到哪些工具定义，仍由父类的渐进式披露
 * 与 {@code HistoryAwareToolSearchAdvisor} 的 restore 逻辑控制，不会破坏 token 优势。
 */
public class HistoryAwareToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate;

    /**
     * sessionId -> 全量工具回调（name -> callback），由 Advisor 在每轮 doBeforeCall 时注册。
     */
    private final Map<String, Map<String, ToolCallback>> sessionCache = new ConcurrentHashMap<>();

    public HistoryAwareToolCallingManager(ToolCallingManager delegate) {
        Assert.notNull(delegate, "delegate ToolCallingManager must not be null");
        this.delegate = delegate;
    }

    /**
     * 注册某会话的全量工具回调，供执行期兜底补全使用。
     */
    public void register(String sessionId, Map<String, ToolCallback> callbacks) {
        if (sessionId != null && !CollectionUtils.isEmpty(callbacks)) {
            this.sessionCache.put(sessionId, callbacks);
        }
    }

    /**
     * 会话结束时清理注册表，避免内存泄漏。
     */
    public void unregister(String sessionId) {
        if (sessionId != null) {
            this.sessionCache.remove(sessionId);
        }
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return this.delegate.resolveToolDefinitions(augment(chatOptions));
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        if (prompt.getOptions() instanceof ToolCallingChatOptions options) {
            ToolCallingChatOptions augmented = augment(options);
            if (augmented != options) {
                prompt = prompt.mutate().chatOptions(augmented).build();
            }
        }
        return this.delegate.executeToolCalls(prompt, chatResponse);
    }

    /**
     * 依据 toolContext 中的 sessionId，把注册表里"当前缺失"的全量工具补回 options。
     * 仅补缺失项，不整体塞回，避免破坏渐进式披露的 token 优势。
     */
    private ToolCallingChatOptions augment(ToolCallingChatOptions options) {
        if (options == null) {
            return options;
        }
        Map<String, Object> toolContext = options.getToolContext();
        if (CollectionUtils.isEmpty(toolContext)) {
            return options;
        }
        Object sessionId = toolContext.get(ToolSearchTool.TOOL_SEARCH_TOOL_SESSION_ID_KEY);
        if (sessionId == null) {
            return options;
        }
        Map<String, ToolCallback> full = this.sessionCache.get(sessionId.toString());
        if (CollectionUtils.isEmpty(full)) {
            return options;
        }

        List<ToolCallback> current = options.getToolCallbacks();
        Set<String> existing = new HashSet<>();
        if (!CollectionUtils.isEmpty(current)) {
            current.forEach(tc -> existing.add(tc.getToolDefinition().name()));
        }

        List<ToolCallback> merged = new ArrayList<>(CollectionUtils.isEmpty(current) ? List.of() : current);
        boolean changed = false;
        for (Map.Entry<String, ToolCallback> entry : full.entrySet()) {
            if (!existing.contains(entry.getKey())) {
                merged.add(entry.getValue());
                changed = true;
            }
        }
        if (!changed) {
            return options;
        }

        return ((ToolCallingChatOptions.Builder<?>) options.mutate())
            .toolCallbacks(merged)
            .toolContext(toolContext)
            .build();
    }
}
