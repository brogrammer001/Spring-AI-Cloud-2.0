package com.mall.aichat.advisor;

import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.utils.DateUtils;
import com.mall.common.core.utils.uuid.IdUtils;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionEventResponseIdGenerator;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * returnDirect 工具结果专用存储 Advisor
 * <p>标记了 returnDirect=true 的工具结果（form/confirm/执行结果 JSON）由 ToolCallingAdvisor
 * 在链头拼好后直接返回给调用方，不会流经任何正数 order 的内层 Advisor——内层的
 * SessionMemoryAdvisor/HistoryChatMemoryAdvisor 既看不到也无法存储它。</p>
 * <p>本 Advisor 必须注册在 {@link Ordered#HIGHEST_PRECEDENCE} + 200，
 * 即 ToolCallingAdvisor（HIGHEST_PRECEDENCE + 300）的外层，才能拦截到 returnDirect 结果：
 * 一份落 MySQL 业务历史表（含 data.tool/data.arguments 等完整工具调用信息，供前端渲染表单/确认卡片），
 * 一份 appendEvent 进 session（内层 SessionMemoryAdvisor 看不到该结果，下一轮 LLM 上下文靠这里补齐，
 * 否则续接对话时模型不知道刚才缺了什么字段/等什么确认）。</p>
 */
public class ReturnDirectChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    /** 必须小于 ToolCallingAdvisor 的 HIGHEST_PRECEDENCE + 300，才能位于其外层拦截 returnDirect 结果 */
    public static final int RETURN_DIRECT_ADVISOR_ORDER = Ordered.HIGHEST_PRECEDENCE + 200;

    private int order = RETURN_DIRECT_ADVISOR_ORDER;

    private StringRedisTemplate stringRedisTemplate;

    private ISysChatHistoryService sysChatHistoryService;

    private SessionService sessionService;

    public ReturnDirectChatMemoryAdvisor(int order, StringRedisTemplate stringRedisTemplate,
                                         ISysChatHistoryService sysChatHistoryService, SessionService sessionService) {
        this.order = order;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysChatHistoryService = sysChatHistoryService;
        this.sessionService = sessionService;
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
        // 逐元素检测：returnDirect 结果是 ToolCallingAdvisor 拼好的单个完整响应元素（非流式增量），
        // 依赖聚合器反而可能受多轮文本拼接干扰
        return chain.nextStream(request)
            .doOnNext(this::persistReturnDirectResult);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // call 同步路径的最终响应同样可能携带 returnDirect generation
        persistReturnDirectResult(chatClientResponse);
        return chatClientResponse;
    }

    private void persistReturnDirectResult(ChatClientResponse chatClientResponse) {
        if (chatClientResponse == null) {
            return;
        }
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) {
            return;
        }
        List<Generation> results = chatResponse.getResults();
        if (results == null || results.isEmpty()) {
            return;
        }

        String conversationId = getConversationId(chatClientResponse.context());
        List<SysChatHistory> histories = new ArrayList<>();
        for (Generation g : results) {
            if (!ToolExecutionResult.FINISH_REASON.equals(g.getMetadata().getFinishReason())) {
                continue;
            }
            // 1. 落 MySQL 业务历史表：content 为完整工具 JSON（含 tool/arguments 工具调用信息）
            SysChatHistory history = new SysChatHistory();
            history.setId(IdUtils.fastUUID());
            history.setConversationId(conversationId);
            history.setContent(g.getOutput().getText());
            history.setTimestamp(new Date());
            history.setType(g.getOutput().getMessageType().getValue());
            history.setSequenceId(stringRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId));
            history.setCreateTime(DateUtils.getNowDate());
            histories.add(history);

            // 2. appendEvent 进 session：内层 SessionMemoryAdvisor 拦不到 returnDirect 结果，
            //    下一轮 LLM 上下文必须靠这里补齐，续接对话（补字段/确认）才有依据
            sessionService.appendEvent(SessionEvent.builder()
                .id(SessionEventResponseIdGenerator.random().generate(chatClientResponse, g.getOutput()))
                .sessionId(conversationId)
                .message(g.getOutput())
                .build());
        }
        if (!histories.isEmpty()) {
            sysChatHistoryService.saveBatch(histories);
        }
    }

    public static ReturnDirectChatMemoryAdvisor.Builder builder(ISysChatHistoryService sysChatHistoryService,
                                                                StringRedisTemplate stringRedisTemplate,
                                                                SessionService sessionService) {
        return new ReturnDirectChatMemoryAdvisor.Builder(sysChatHistoryService, stringRedisTemplate, sessionService);
    }

    public static final class Builder {

        private int order = RETURN_DIRECT_ADVISOR_ORDER;

        private StringRedisTemplate stringRedisTemplate;

        private ISysChatHistoryService sysChatHistoryService;

        private SessionService sessionService;

        private Builder(ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate,
                        SessionService sessionService) {
            Assert.notNull(sysChatHistoryService, "chatHistoryService cannot be null");
            Assert.notNull(sessionService, "sessionService cannot be null");
            this.sysChatHistoryService = sysChatHistoryService;
            this.stringRedisTemplate = stringRedisTemplate;
            this.sessionService = sessionService;
        }

        /**
         * Set the order. 注意：必须保持小于 ToolCallingAdvisor 的 HIGHEST_PRECEDENCE + 300，
         * 否则将落入工具调用内循环，永远拦截不到 returnDirect 结果。
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
            return new ReturnDirectChatMemoryAdvisor(this.order, this.stringRedisTemplate, this.sysChatHistoryService, this.sessionService);
        }

    }
}
