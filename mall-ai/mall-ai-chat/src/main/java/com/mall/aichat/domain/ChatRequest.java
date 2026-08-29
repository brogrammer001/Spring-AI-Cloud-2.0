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

    /** 本次回复的消息ID */
    private final String messageId;

    /** 流式 usage 统计载体 */
    private final AtomicReference<Usage> usageRef = new AtomicReference<>();

    private ChatRequest(Builder builder) {
        this.question = builder.question;
        this.conversationId = builder.conversationId;
        this.messageId = UUID.randomUUID().toString();
    }

    public String getQuestion() {
        return question;
    }

    public String getConversationId() {
        return conversationId;
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

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(this);
        }
    }
}
