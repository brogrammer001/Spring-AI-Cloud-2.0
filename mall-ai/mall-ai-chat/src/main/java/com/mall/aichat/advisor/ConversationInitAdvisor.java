package com.mall.aichat.advisor;

import com.mall.aichat.service.IAiConversationService;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

/**
 * 拦截请求：在请求发送给大模型前，判断是新建会话还是已有会话。
 * 权限校验与初始化：校验权限，若为新会话则生成 ID、生成标题、落库。
 * 上下文传递：确保生成的 conversationId 能传给后续的 MessageChatMemoryAdvisor 使用。
 */
public class ConversationInitAdvisor implements StreamAdvisor {

    private final IAiConversationService aiConversationService;

    // 构造器
    public ConversationInitAdvisor(IAiConversationService aiConversationService) {
        this.aiConversationService = aiConversationService;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // 1. 获取当前登录用户ID (假设通过上下文传递，或直接调用 SecurityUtils)
        String conversationId = (String) chatClientRequest.context().get(ChatMemory.CONVERSATION_ID);
        Long userId = (Long) chatClientRequest.context().get("userId");

        // --- 已有会话逻辑 ---
        boolean hasPermission = aiConversationService.checkConversationOwner(userId, conversationId);
        if (!hasPermission) {
            // 在流式响应中抛出异常，全局异常处理器会捕获并返回错误流
            throw new SecurityException("无权访问该会话");
        }

        // 3. 更新请求上下文
        // 将确定好的 conversationId 放入上下文，后续的 ChatMemoryAdvisor 会读取这个值
        chatClientRequest.context().put(ChatMemory.CONVERSATION_ID, conversationId);

        // 4. 放行请求，继续执行后续的 Advisor 和大模型调用
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    @Override
    public String getName() {
        return "请求前初始化";
    }

    @Override
    public int getOrder() {
        // 必须在 MessageChatMemoryAdvisor 之前执行，因为我们要先生成 conversationId
        // MessageChatMemoryAdvisor 的默认顺序通常较低，这里设为 0 确保优先执行
        return 0;
    }
}
