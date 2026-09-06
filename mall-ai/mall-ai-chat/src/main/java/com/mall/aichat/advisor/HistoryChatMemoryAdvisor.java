package com.mall.aichat.advisor;

import com.mall.aichat.config.AgentEventSinkManager;
import com.mall.aichat.domain.AiAgentToolCallLog;
import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.IAiAgentToolCallLogService;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.utils.DateUtils;
import com.mall.common.core.utils.uuid.IdUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.stream.Stream;

public class HistoryChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private int order;

    private StringRedisTemplate stringRedisTemplate;

    private ISysChatHistoryService sysChatHistoryService;

    private AgentEventSinkManager agentEventSinkManager;

    private IAiAgentToolCallLogService logService;

    private JsonMapper jsonMapper = JsonMapper.builder().build();

    public HistoryChatMemoryAdvisor(int order, StringRedisTemplate stringRedisTemplate, ISysChatHistoryService sysChatHistoryService,
                                    AgentEventSinkManager agentEventSinkManager, IAiAgentToolCallLogService logService) {
        this.order = order;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysChatHistoryService = sysChatHistoryService;
        this.logService = logService;
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
        String userId = Objects.requireNonNull(chatClientRequest.context().get(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY)).toString();
        Message userMessage = chatClientRequest.prompt().getLastUserOrToolResponseMessage();
        this.saveHistory(conversationId, Collections.singletonList(userMessage));
        this.saveToolCallLog(conversationId, userId, Collections.singletonList(userMessage));
        // 返回处理后的请求，确保注入的内存消息真正生效（而非返回原始请求）
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        String conversationId = getConversationId(chatClientResponse.context());

        String userId = Objects.requireNonNull(chatClientResponse.context().get(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY)).toString();

        List<Message> assistantMessages = new ArrayList<>();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse.chatResponse().getResults().stream()
                .map(g -> (Message) g.getOutput())
                .toList();
        }
        this.saveHistory(conversationId, assistantMessages);
        this.saveToolCallLog(conversationId, userId, assistantMessages);
        return chatClientResponse;
    }


    private void saveToolCallLog(String conversationId, String userId, List<Message> messageList) {
        List<AiAgentToolCallLog> list = messageList.stream()
            .filter(message -> (message instanceof AssistantMessage am && am.hasToolCalls()) || message instanceof ToolResponseMessage)
            .filter(message -> !StringUtils.hasText(message.getText()))
            .flatMap(message -> {
                if (message instanceof AssistantMessage) {
                    List<AssistantMessage.ToolCall> toolCalls = ((AssistantMessage) message).getToolCalls();
                    return toolCalls.stream().map(toolCall -> {
                        AiAgentToolCallLog pendingLog = new AiAgentToolCallLog();
                        pendingLog.setCallId(UUID.randomUUID().toString());
                        pendingLog.setConversationId(conversationId);
                        pendingLog.setUserId(Long.valueOf(userId));
                        pendingLog.setToolName(toolCall.name());
                        pendingLog.setToolParams(toolCall.arguments());
                        pendingLog.setResultRef(messageDataToJson(message));
                        pendingLog.setStatus("SUCCESS");
                        pendingLog.setCreateTime(DateUtils.getNowDate());
                        pendingLog.setCreateBy(userId);
                        return pendingLog;
                    });
                }

                if (message instanceof ToolResponseMessage) {
                    List<ToolResponseMessage.ToolResponse> toolCalls = ((ToolResponseMessage) message).getResponses();
                    return toolCalls.stream().map(toolCall -> {
                        AiAgentToolCallLog pendingLog = new AiAgentToolCallLog();
                        pendingLog.setCallId(UUID.randomUUID().toString());
                        pendingLog.setConversationId(conversationId);
                        pendingLog.setUserId(Long.valueOf(userId));
                        pendingLog.setToolName(toolCall.name());
                        pendingLog.setResultDigest(toolCall.responseData());
                        pendingLog.setResultRef(messageDataToJson(message));
                        pendingLog.setStatus("SUCCESS");
                        pendingLog.setCreateTime(DateUtils.getNowDate());
                        pendingLog.setCreateBy(userId);
                        return pendingLog;
                    });
                }

                return Stream.empty();
            }).toList();

        if (list.isEmpty()) return;
        logService.saveBatch(list);
    }

    @Nullable
    private String toJson(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        try {
            return this.jsonMapper.writeValueAsString(value);
        }
        catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize value to JSON", ex);
        }
    }

    /**
     * Serializes type-specific {@link Message} payload to JSON:
     * <ul>
     * <li>{@link AssistantMessage} with tool calls → JSON array of tool calls</li>
     * <li>{@link ToolResponseMessage} → JSON array of tool responses</li>
     * <li>All other types → {@code null}</li>
     * </ul>
     */
    @Nullable private String messageDataToJson(Message message) {
        if (message instanceof AssistantMessage am && am.hasToolCalls()) {
            return toJson(am.getToolCalls());
        }
        if (message instanceof ToolResponseMessage trm) {
            return toJson(trm.getResponses());
        }
        return null;
    }

    private void saveHistory(String conversationId, List<Message> messageList) {
        List<SysChatHistory> list = messageList.stream()
            .filter(message -> StringUtils.hasText(message.getText()))
            .map(message -> {
                SysChatHistory history = new SysChatHistory();
                history.setId(IdUtils.fastUUID());
                history.setConversationId(conversationId);
                history.setTimestamp(new Date());
                history.setType(message.getMessageType().getValue());
                history.setSequenceId(stringRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId));
                history.setCreateTime(DateUtils.getNowDate());
                history.setContent(message.getText());
                return history;
            }).toList();

        if (list.isEmpty()) return;
        sysChatHistoryService.saveBatch(list);
    }

    public static HistoryChatMemoryAdvisor.Builder builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate,
                                                           AgentEventSinkManager agentEventSinkManager, IAiAgentToolCallLogService logService) {
        return new HistoryChatMemoryAdvisor.Builder(sysChatHistoryService, stringRedisTemplate, agentEventSinkManager, logService);
    }

    public static final class Builder {

        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

        private StringRedisTemplate stringRedisTemplate;

        private ISysChatHistoryService sysChatHistoryService;

        private AgentEventSinkManager agentEventSinkManager;

        private IAiAgentToolCallLogService logService;

        private Builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate,
                        AgentEventSinkManager agentEventSinkManager, IAiAgentToolCallLogService logService) {
            Assert.notNull(sysChatHistoryService, "chatMemory cannot be null");
            this.agentEventSinkManager = agentEventSinkManager;
            this.sysChatHistoryService = sysChatHistoryService;
            this.stringRedisTemplate = stringRedisTemplate;
            this.logService = logService;
        }

        /**
         * Set the order.
         *
         * @param order the order
         * @return the builder
         */
        public HistoryChatMemoryAdvisor.Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * Build the advisor.
         *
         * @return the advisor
         */
        public HistoryChatMemoryAdvisor build() {
            return new HistoryChatMemoryAdvisor(this.order, this.stringRedisTemplate, this.sysChatHistoryService, this.agentEventSinkManager, this.logService);
        }

    }
}