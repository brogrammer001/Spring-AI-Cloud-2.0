package com.mall.aichat.advisor;

import com.alibaba.fastjson2.JSON;
import com.mall.aichat.config.AgentEventSinkManager;
import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.utils.uuid.IdUtils;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Stream;

public class FullHistoryChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private int order;

    private StringRedisTemplate stringRedisTemplate;

    private ISysChatHistoryService sysChatHistoryService;

    private AgentEventSinkManager agentEventSinkManager;

    private final ChatMemory chatMemory;

    public FullHistoryChatMemoryAdvisor(int order, StringRedisTemplate stringRedisTemplate, ISysChatHistoryService sysChatHistoryService,
                                        ChatMemory chatMemory, AgentEventSinkManager agentEventSinkManager) {
        this.order = order;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysChatHistoryService = sysChatHistoryService;
        this.chatMemory = chatMemory;
        this.agentEventSinkManager = agentEventSinkManager;
    }

    @Override
    public String getName() {
        return "全量消息存储";
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 1. 获取会话ID
        String conversationId = getConversationId(request.context());
        return Mono.just(request)
            .publishOn(BaseAdvisor.DEFAULT_SCHEDULER)
            .map(chatClientRequest -> this.before(chatClientRequest, chain))
            .flatMapMany(chain::nextStream)
            .flatMap(chatClientResponse -> {
                if (chatClientResponse.chatResponse() == null || chatClientResponse.chatResponse().getResults().isEmpty()) {
                    return Flux.just(chatClientResponse);
                }

                Generation generation = chatClientResponse.chatResponse().getResults().getFirst();

                // 1. 拦截大模型发起的工具调用指令
                AssistantMessage am = generation.getOutput();
                if (am.hasToolCalls()) {
                    // 遍历推送所有工具调用的 calling 状态，避免并行工具只推送第一个
                    am.getToolCalls().forEach(toolCall ->
                        agentEventSinkManager.emitThought(conversationId, toolCall.name()));
                }
                return Flux.just(chatClientResponse);
            })
            .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
                chatClientResponse -> this.after(chatClientResponse, chain)));
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String conversationId = getConversationId(chatClientRequest.context());
        List<Message> memoryMessages = this.chatMemory.get(conversationId);

        List<Message> promptMessages = chatClientRequest.prompt().getInstructions();
        List<Message> processedMessages = new ArrayList<>();
        if (!isMemoryAlreadyInPrompt(promptMessages, memoryMessages)) {
            processedMessages.addAll(memoryMessages);
        }
        processedMessages.addAll(promptMessages);

        // 2.1. Ensure system message, if present, appears first in the list.
        for (int i = 0; i < processedMessages.size(); i++) {
            if (processedMessages.get(i) instanceof SystemMessage) {
                Message systemMessage = processedMessages.remove(i);
                processedMessages.addFirst(systemMessage);
                break;
            }
        }

        // 3. Create a new request with the advised messages.
        ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
            .prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
            .build();

        // 4. Add the new user message to the conversation memory.
        Message userMessage = processedChatClientRequest.prompt().getLastUserOrToolResponseMessage();

        this.saveHistory(conversationId, Collections.singletonList(userMessage));
        // 返回处理后的请求，确保注入的内存消息真正生效（而非返回原始请求）
        return processedChatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        String conversationId = getConversationId(chatClientResponse.context());
        List<Message> assistantMessages = new ArrayList<>();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse.chatResponse()
                .getResults()
                .stream()
                .map(g -> (Message) g.getOutput())
                .toList();
        }
        this.saveHistory(conversationId, assistantMessages);
        return chatClientResponse;
    }


    private void saveHistory(String conversationId, List<Message> messageList) {
        List<SysChatHistory> list = messageList.stream().flatMap(message -> {
            if (message instanceof ToolResponseMessage trm) {
                // 工具执行结果：使用 toolName / toolResult 字段语义化存储
                return trm.getResponses().stream().map(r -> {
                    Long sequenceId = stringRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId);
                    SysChatHistory history = new SysChatHistory();
                    history.setId(IdUtils.fastUUID());
                    history.setConversationId(conversationId);
                    history.setContent(r.responseData());
                    history.setToolName(r.name());
                    history.setToolResult(r.responseData());
                    history.setTimestamp(new Date());
                    history.setIsCompression("N");
                    history.setType(message.getMessageType().getValue());
                    history.setSequenceId(sequenceId);
                    return history;
                });
            } else {
                Long sequenceId = stringRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId);
                SysChatHistory history = new SysChatHistory();
                history.setId(IdUtils.fastUUID());
                history.setConversationId(conversationId);
                history.setContent(message.getText());

                // 如果是包含工具调用的 AssistantMessage，必须把 ToolCalls 序列化存入
                if (message instanceof AssistantMessage am && am.hasToolCalls()) {
                    // toolCalls 保存工具调用参数 JSON（模型发起调用的输入）
                    history.setToolCalls(JSON.toJSONString(am.getToolCalls()));
                    // content 优先保留 reasoningContent（思考过程），为空时回退到 am.getText()
                    Map<String, Object> metadata = am.getMetadata();
                    String reasoningContent = (String) metadata.get("reasoningContent");
                    if (StringUtils.hasText(reasoningContent)) {
                        history.setContent(reasoningContent);
                    }
                }
                history.setTimestamp(new Date());
                history.setIsCompression("N");
                history.setType(message.getMessageType().getValue());
                history.setSequenceId(sequenceId);
                return Stream.of(history);
            }
        }).toList();

        if (list.isEmpty()) return;
        sysChatHistoryService.saveBatch(list);
    }

    private static boolean isMemoryAlreadyInPrompt(List<Message> promptMessages, List<Message> memoryMessages) {
        if (memoryMessages.isEmpty()) {
            return true;
        }
        if (promptMessages.size() < memoryMessages.size()) {
            return false;
        }
        for (int offset = 0; offset <= promptMessages.size() - memoryMessages.size(); offset++) {
            if (startsWith(promptMessages, memoryMessages, offset)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(List<Message> messages, List<Message> prefix, int offset) {
        if (messages.size() - offset < prefix.size()) {
            return false;
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (!messages.get(i + offset).equals(prefix.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static FullHistoryChatMemoryAdvisor.Builder builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate,
                                                               ChatMemory chatMemory, AgentEventSinkManager agentEventSinkManager) {
        return new FullHistoryChatMemoryAdvisor.Builder(sysChatHistoryService, stringRedisTemplate, chatMemory, agentEventSinkManager);
    }

    public static final class Builder {

        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

        private StringRedisTemplate stringRedisTemplate;

        private ISysChatHistoryService sysChatHistoryService;

        private AgentEventSinkManager agentEventSinkManager;

        private final ChatMemory chatMemory;

        private Builder(ISysChatHistoryService sysChatHistoryService,StringRedisTemplate stringRedisTemplate, ChatMemory chatMemory, AgentEventSinkManager agentEventSinkManager) {
            Assert.notNull(sysChatHistoryService, "chatMemory cannot be null");
            this.chatMemory = chatMemory;
            this.sysChatHistoryService = sysChatHistoryService;
            this.stringRedisTemplate = stringRedisTemplate;
            this.agentEventSinkManager = agentEventSinkManager;
        }

        /**
         * Set the order.
         *
         * @param order the order
         * @return the builder
         */
        public FullHistoryChatMemoryAdvisor.Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * Build the advisor.
         *
         * @return the advisor
         */
        public FullHistoryChatMemoryAdvisor build() {
            return new FullHistoryChatMemoryAdvisor(this.order, this.stringRedisTemplate, this.sysChatHistoryService, this.chatMemory, this.agentEventSinkManager);
        }

    }
}