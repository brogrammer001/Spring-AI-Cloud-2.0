package com.mall.aichat.domain;

import org.springframework.ai.chat.metadata.Usage;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 聊天流式请求对象
 * <p>携带请求参数与流中共享状态：usage 统计由 Service 流式过程写入，
 * Controller 构建 message_end 事件时读取</p>
 *
 * @author mall
 */
public class ChatRequest {

    /** 用户问题 */
    private final String question;

    /** 会话ID（Controller 中已归一化，保证非空） */
    private final String conversationId;

    /** 用户标识（Controller 中已归一化，缺失时为 anonymous），长期记忆跨会话作用域 */
    private final String userId;

    /** 租户/部门隔离标识（多租户场景下 Advisor 和工具审计据此做硬隔离） */
    private final String tenantId;

    /** 部门 ID（数据权限过滤） */
    private final Long deptId;

    /** 本次请求追踪 ID（贯穿所有工具调用日志） */
    private final String traceId;

    /** 本次回复的消息ID */
    private final String messageId;

    /** 流式 usage 统计载体 */
    private final AtomicReference<Usage> usageRef = new AtomicReference<>();

    private ChatRequest(Builder builder) {
        this.question = builder.question;
        this.conversationId = builder.conversationId;
        this.userId = builder.userId;
        this.tenantId = builder.tenantId;
        this.deptId = builder.deptId;
        this.traceId = builder.traceId != null ? builder.traceId : UUID.randomUUID().toString();
        this.messageId = UUID.randomUUID().toString();
    }

    public String getQuestion() {
        return question;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getMessageId() {
        return messageId;
    }

    public AtomicReference<Usage> getUsageRef() {
        return usageRef;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String question;
        private String conversationId;
        private String userId;
        private String tenantId;
        private Long deptId;
        private String traceId;

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder deptId(Long deptId) {
            this.deptId = deptId;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(this);
        }
    }
}
