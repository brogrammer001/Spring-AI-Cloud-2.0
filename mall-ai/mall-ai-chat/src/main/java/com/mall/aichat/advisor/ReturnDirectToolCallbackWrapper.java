package com.mall.aichat.advisor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.util.StringUtils;

public class ReturnDirectToolCallbackWrapper implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolMetadata wrappedMetadata;

    public ReturnDirectToolCallbackWrapper(ToolCallback delegate) {
        this.delegate = delegate;

        // 在构造时就解析 inputSchema 并决定是否开启 returnDirect
        boolean isReturnDirect = checkReturnDirectFromSchema(delegate.getToolDefinition());

        // 构建新的 ToolMetadata，保留原有的其他属性，覆盖 returnDirect
        this.wrappedMetadata = ToolMetadata.builder()
            .returnDirect(isReturnDirect)      // 强制设置我们解析出的值
            .build();
    }


    @Override
    public @NonNull ToolDefinition getToolDefinition() {
        // 定义不需要改，直接返回
        return delegate.getToolDefinition();
    }

    @Override
    public @NonNull ToolMetadata getToolMetadata() {
        // 【核心拦截】返回我们构建好的、带有 returnDirect 的元数据
        return this.wrappedMetadata;
    }

    @Override
    public @NonNull String call(@NonNull String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public @NonNull String call(@NonNull String toolInput, @Nullable ToolContext toolContext) {
        return delegate.call(toolInput, toolContext);
    }

    /**
     * 从 InputSchema JSON 字符串中提取 returnDirect 标记
     */
    private boolean checkReturnDirectFromSchema(ToolDefinition definition) {
        String description = definition.description();
        if (!StringUtils.hasText(description)) {
            return false;
        }

        try {
            return description.endsWith("[JSON]");
        } catch (Exception e) {
            // 解析失败默认返回 false
            return false;
        }
    }
}
