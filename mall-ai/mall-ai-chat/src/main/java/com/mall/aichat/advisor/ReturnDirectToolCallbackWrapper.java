package com.mall.aichat.advisor;

import com.mall.aichat.service.impl.ToolDataCacheService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.util.StringUtils;

public class ReturnDirectToolCallbackWrapper implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ReturnDirectToolCallbackWrapper.class);

    private final ToolCallback delegate;
    private final ToolMetadata wrappedMetadata;
    private final ToolDataCacheService dataCacheService;

    public ReturnDirectToolCallbackWrapper(ToolCallback delegate) {
        this(delegate, null);
    }

    public ReturnDirectToolCallbackWrapper(ToolCallback delegate, ToolDataCacheService dataCacheService) {
        this.delegate = delegate;
        this.dataCacheService = dataCacheService;

        boolean isReturnDirect = checkReturnDirectFromSchema(delegate.getToolDefinition());
        this.wrappedMetadata = ToolMetadata.builder()
            .returnDirect(isReturnDirect)
            .build();
    }

    @Override
    public @NonNull ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public @NonNull ToolMetadata getToolMetadata() {
        return this.wrappedMetadata;
    }

    @Override
    public @NonNull String call(@NonNull String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public @NonNull String call(@NonNull String toolInput, @Nullable ToolContext toolContext) {
        String toolName = delegate.getToolDefinition().name();

        // 【输入拦截】如果参数中包含 dataId，替换为Redis中的真实数据
        String resolvedInput = toolInput;
        if (dataCacheService != null && toolInput.contains("dataId:")) {
            resolvedInput = dataCacheService.resolveDataIds(toolInput);
            log.info("工具[{}]输入拦截：dataId已解析", toolName);
        }

        // 执行原始工具
        String result = delegate.call(resolvedInput, toolContext);

        // 【输出拦截】如果结果较大，缓存到Redis，返回dataId引用
        if (dataCacheService != null) {
            result = dataCacheService.cacheIfLarge(toolName, result);
        }
        return result;
    }

    private boolean checkReturnDirectFromSchema(ToolDefinition definition) {
        String description = definition.description();
        if (!StringUtils.hasText(description)) {
            return false;
        }
        try {
            return description.endsWith("[JSON]");
        } catch (Exception e) {
            return false;
        }
    }
}
