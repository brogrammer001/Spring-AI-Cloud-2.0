package com.mall.aichat.advisor;

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
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReturnDirectChatMemoryAdvisor implements StreamAdvisor {

    private int order;

    private StringRedisTemplate stringRedisTemplate;

    private ISysChatHistoryService sysChatHistoryService;

    private final ChatMemory chatMemory;

    public ReturnDirectChatMemoryAdvisor(int order, StringRedisTemplate stringRedisTemplate, ISysChatHistoryService sysChatHistoryService, ChatMemory chatMemory) {
        this.order = order;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysChatHistoryService = sysChatHistoryService;
        this.chatMemory = chatMemory;
    }

    @Override
    public String getName() {
        return "ReturnDirectChatMemoryAdvisor";
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
            .flatMapMany(chain::nextStream)
            .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
                chatClientResponse -> this.after(chatClientResponse, conversationId)));
    }

    private void saveHistory(String conversationId, List<Message> messageList) {
        List<SysChatHistory> list = messageList.stream().map(message -> {
            Long sequenceId = stringRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId);
            SysChatHistory history = new SysChatHistory();
            history.setId(IdUtils.fastUUID());
            history.setConversationId(conversationId);
            history.setContent(message.getText());
            history.setTimestamp(new Date());
            history.setIsCompression("N");
            history.setType(message.getMessageType().getValue());
            history.setSequenceId(sequenceId);
            return history;
        }).toList();
        if (list.isEmpty()) return;
        sysChatHistoryService.saveBatch(list);
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

    public static ReturnDirectChatMemoryAdvisor.Builder builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate, ChatMemory chatMemory) {
        return new ReturnDirectChatMemoryAdvisor.Builder(sysChatHistoryService, stringRedisTemplate, chatMemory);
    }

    public static final class Builder {

        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

        private StringRedisTemplate stringRedisTemplate;

        private ISysChatHistoryService sysChatHistoryService;

        private final ChatMemory chatMemory;

        private Builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate, ChatMemory chatMemory) {
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
        public ReturnDirectChatMemoryAdvisor.Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * Build the advisor.
         *
         * @return the advisor
         */
        public ReturnDirectChatMemoryAdvisor build() {
            return new ReturnDirectChatMemoryAdvisor(this.order, this.stringRedisTemplate, this.sysChatHistoryService, this.chatMemory);
        }

    }
}
