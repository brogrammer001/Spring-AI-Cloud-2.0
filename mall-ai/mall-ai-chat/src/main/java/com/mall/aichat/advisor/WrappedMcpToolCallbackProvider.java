package com.mall.aichat.advisor;

import com.mall.aichat.service.impl.ToolDataCacheService;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;

public class WrappedMcpToolCallbackProvider implements ToolCallbackProvider {

    private final AsyncMcpToolCallbackProvider delegate;
    private final ToolDataCacheService dataCacheService;

    public WrappedMcpToolCallbackProvider(AsyncMcpToolCallbackProvider delegate) {
        this(delegate, null);
    }

    public WrappedMcpToolCallbackProvider(AsyncMcpToolCallbackProvider delegate, ToolDataCacheService dataCacheService) {
        this.delegate = delegate;
        this.dataCacheService = dataCacheService;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        ToolCallback[] originalCallbacks = delegate.getToolCallbacks();

        return Arrays.stream(originalCallbacks)
            .map(cb -> new ReturnDirectToolCallbackWrapper(cb, dataCacheService))
            .toArray(ToolCallback[]::new);
    }
}
