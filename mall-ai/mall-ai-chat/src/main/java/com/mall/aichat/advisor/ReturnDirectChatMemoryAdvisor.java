package com.mall.aichat.advisor;

import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.utils.uuid.IdUtils;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

public class ReturnDirectChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private int order;

    private StringRedisTemplate stringRedisTemplate;

    private ISysChatHistoryService sysChatHistoryService;

    public ReturnDirectChatMemoryAdvisor(int order, StringRedisTemplate stringRedisTemplate, ISysChatHistoryService sysChatHistoryService) {
        this.order = order;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysChatHistoryService = sysChatHistoryService;
    }

    @Override
    public String getName() {
        return "针对MCP工具标记了【returnDirect】=true的结果存储";
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return Mono.just(request)
            .publishOn(BaseAdvisor.DEFAULT_SCHEDULER)
            .flatMapMany(chain::nextStream)
            .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
                chatClientResponse -> this.after(chatClientResponse, chain)));
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        String conversationId = getConversationId(chatClientResponse.context());
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) return chatClientResponse;

        Generation result = chatResponse.getResult();
        ChatGenerationMetadata generationMetadata = result.getMetadata();
        String finishReason = generationMetadata.getFinishReason();

        if ("returnDirect".equals(finishReason)) {
            List<Message> assistantMessages = chatClientResponse.chatResponse()
                .getResults()
                .stream()
                .map(g -> (Message) g.getOutput())
                .toList();

            List<SysChatHistory> list = assistantMessages.stream().map(message -> {
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
            if (list.isEmpty()) return chatClientResponse;
            sysChatHistoryService.saveBatch(list);
        }
        return chatClientResponse;
    }

    public static ReturnDirectChatMemoryAdvisor.Builder builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate) {
        return new ReturnDirectChatMemoryAdvisor.Builder(sysChatHistoryService, stringRedisTemplate);
    }

    public static final class Builder {

        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

        private StringRedisTemplate stringRedisTemplate;

        private ISysChatHistoryService sysChatHistoryService;


        private Builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate) {
            Assert.notNull(sysChatHistoryService, "chatMemory cannot be null");
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
            return new ReturnDirectChatMemoryAdvisor(this.order, this.stringRedisTemplate, this.sysChatHistoryService);
        }

    }
}
