package com.mall.aichat.advisor;

import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;

public class WrappedMcpToolCallbackProvider implements ToolCallbackProvider {

    private final AsyncMcpToolCallbackProvider delegate;

    // 修改构造函数：直接接收已经配置好（包含 Filter、Prefix 等）的 Provider
    public WrappedMcpToolCallbackProvider(AsyncMcpToolCallbackProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        // 1. 获取原始工具列表（此时 Filter 和 Prefix 已经生效了）
        ToolCallback[] originalCallbacks = delegate.getToolCallbacks();

        // 2. 遍历并用我们的 Wrapper 包装每一个工具（注入 returnDirect）
        return Arrays.stream(originalCallbacks)
            .map(ReturnDirectToolCallbackWrapper::new)
            .toArray(ToolCallback[]::new);
    }
}
