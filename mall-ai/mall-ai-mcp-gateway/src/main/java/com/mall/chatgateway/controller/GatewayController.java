package com.mall.chatgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {
    private final ToolCallbackProvider toolCallbackProvider;
    private final static Logger log = LoggerFactory.getLogger(GatewayController.class);

    public GatewayController(ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    /**
     * 列出所有从 Nacos 发现并聚合的 MCP 工具
     */
    @GetMapping("/tools")
    public List<Map<String, String>> listTools() {
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        return Arrays.stream(toolCallbacks)
            .map(tool -> Map.of(
                "name", tool.getToolDefinition().name(),
                "description", tool.getToolDefinition().description()
            ))
            .collect(Collectors.toList());
    }
}
