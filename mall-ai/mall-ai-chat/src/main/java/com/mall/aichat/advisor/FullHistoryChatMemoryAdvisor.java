package com.mall.aichat.advisor;

import com.alibaba.fastjson2.JSON;
import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.utils.uuid.IdUtils;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Stream;

public class FullHistoryChatMemoryAdvisor implements StreamAdvisor {

    private int order;

    private StringRedisTemplate stringRedisTemplate;

    private ISysChatHistoryService sysChatHistoryService;

    private final ChatMemory chatMemory;

    public FullHistoryChatMemoryAdvisor(int order, StringRedisTemplate stringRedisTemplate, ISysChatHistoryService sysChatHistoryService, ChatMemory chatMemory) {
        this.order = order;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysChatHistoryService = sysChatHistoryService;
        this.chatMemory = chatMemory;
    }

    @Override
    public String getName() {
        return "FullHistoryChatMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 1. 获取会话ID
        String conversationId = (String) request.context().get(ChatMemory.CONVERSATION_ID);
        return Mono.just(request)
            .publishOn(BaseAdvisor.DEFAULT_SCHEDULER)
            .map(chatClientRequest -> this.before(chatClientRequest, conversationId))
            .flatMapMany(chain::nextStream)
            .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
                chatClientResponse -> this.after(chatClientResponse, conversationId)));
    }

    private ChatClientRequest before(ChatClientRequest request, String conversationId) {
        // 1. Retrieve the chat memory for the current conversation.
        List<Message> memoryMessages = this.chatMemory.get(conversationId);

        // 2. Advise the request messages list.
        List<Message> promptMessages = request.prompt().getInstructions();
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
        ChatClientRequest processedChatClientRequest = request.mutate()
            .prompt(request.prompt().mutate().messages(processedMessages).build())
            .build();

        // 4. Add the new user message to the conversation memory.
        Message userMessage = processedChatClientRequest.prompt().getLastUserOrToolResponseMessage();

        this.saveHistory(conversationId, Collections.singletonList(userMessage));
        return request;
    }

    private void saveHistory(String conversationId, List<Message> messageList) {
        List<SysChatHistory> list = messageList.stream().flatMap(message -> {
            if (message instanceof ToolResponseMessage trm) {
                return trm.getResponses().stream().map(r -> {
                    Long sequenceId = stringRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId);
                    SysChatHistory history = new SysChatHistory();
                    history.setId(IdUtils.fastUUID());
                    history.setConversationId(conversationId);
                    history.setContent(r.name());
                    history.setToolCalls(r.responseData());
                    history.setTimestamp(new Date());
                    history.setIsCompression("N");
                    history.setType(message.getMessageType().getValue());
                    history.setSequenceId(sequenceId);
                    return history;
                });
            }else {
                Long sequenceId = stringRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId);
                SysChatHistory history = new SysChatHistory();
                history.setId(IdUtils.fastUUID());
                history.setConversationId(conversationId);
                history.setContent(message.getText());

                // 如果是包含工具调用的 AssistantMessage，必须把 ToolCalls 序列化存入
                if (message instanceof AssistantMessage am && am.hasToolCalls()) {
                    // 注意：你需要把 toolCalls 转换成 JSON 字符串存下来
                    history.setToolCalls(JSON.toJSONString(am.getToolCalls()));
                    Map<String, Object> metadata = am.getMetadata();

                    history.setContent((String) metadata.get("reasoningContent"));
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

    public void after(ChatClientResponse chatClientResponse, String conversationId) {
        List<Message> assistantMessages = new ArrayList<>();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse.chatResponse()
                .getResults()
                .stream()
                .map(g -> (Message) g.getOutput())
                .toList();
        }
        this.saveHistory(conversationId, assistantMessages);
    }

    public static FullHistoryChatMemoryAdvisor.Builder builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate, ChatMemory chatMemory) {
        return new FullHistoryChatMemoryAdvisor.Builder(sysChatHistoryService, stringRedisTemplate, chatMemory);
    }

    public static final class Builder {

        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

        private StringRedisTemplate stringRedisTemplate;

        private ISysChatHistoryService sysChatHistoryService;

        private final ChatMemory chatMemory;

        private Builder(ISysChatHistoryService sysChatHistoryService,StringRedisTemplate stringRedisTemplate, ChatMemory chatMemory) {
            Assert.notNull(sysChatHistoryService, "chatMemory cannot be null");
            this.chatMemory = chatMemory;
            this.sysChatHistoryService = sysChatHistoryService;
            this.stringRedisTemplate = stringRedisTemplate;
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
            return new FullHistoryChatMemoryAdvisor(this.order, this.stringRedisTemplate, this.sysChatHistoryService, this.chatMemory);
        }

    }
}
