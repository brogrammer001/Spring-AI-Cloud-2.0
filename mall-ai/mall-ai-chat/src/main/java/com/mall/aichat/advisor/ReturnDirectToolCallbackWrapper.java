package com.mall.aichat.advisor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReturnDirectToolCallbackWrapper(ToolCallback delegate) {
        this.delegate = delegate;

        // 在构造时就解析工具描述并决定是否开启 returnDirect
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
        String result = delegate.call(toolInput, toolContext);
        // returnDirect 工具的结果直出前端，为 type=form 的表单响应补充重放上下文
        if (wrappedMetadata.returnDirect()) {
            return enrichFormResult(result, toolInput);
        }
        return result;
    }

    /**
     * 从工具描述中提取 returnDirect 标记
     */
    private boolean checkReturnDirectFromSchema(ToolDefinition definition) {
        String description = definition.description();
        if (!StringUtils.hasText(description)) {
            return false;
        }

        try {
            // 描述中带 [JSON] 标记的工具结果直接返回前端（不经过模型总结）
            return description.contains("[JSON]");
        } catch (Exception e) {
            // 解析失败默认返回 false
            return false;
        }
    }

    /**
     * 为 type=form 的表单响应补充 tool 与 arguments 上下文，
     * 前端据此在用户填写完成后携带合并参数重新调用该工具
     */
    private String enrichFormResult(String result, String toolInput) {
        try {
            JsonNode root = objectMapper.readTree(result);
            if (root == null || !root.isObject()) {
                return result;
            }
            JsonNode data = root.get("data");
            if (data == null || !data.isObject() || !"form".equals(data.path("type").asText())) {
                return result;
            }
            ObjectNode dataNode = (ObjectNode) data;
            if (!dataNode.has("tool")) {
                dataNode.put("tool", delegate.getToolDefinition().name());
            }
            if (!dataNode.has("arguments") && StringUtils.hasText(toolInput)) {
                JsonNode input = objectMapper.readTree(toolInput);
                if (input.isObject()) {
                    // 剔除 confirmed，重放时由前端/用户确认后再带上
                    ((ObjectNode) input).remove("confirmed");
                    dataNode.set("arguments", input);
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            // 解析失败原样返回
            return result;
        }
    }
}
