package com.mall.aichat.constant;

/**
 * AI 对话流式交互常量信息
 *
 * @author mall
 */
public class ChatConstants
{
    /**
     * SSE 事件名：增量消息
     */
    public static final String EVENT_MESSAGE = "message";

    /**
     * SSE 事件名：消息结束
     */
    public static final String EVENT_MESSAGE_END = "message_end";

    /**
     * SSE 事件名：错误
     */
    public static final String EVENT_ERROR = "error";

    /**
     * SSE 事件名：工具调用
     */
    public static final String EVENT_TOOL_CALL = "tool_call";

    /**
     * SSE 事件名：RAG 检索状态
     */
    public static final String EVENT_RAG_RETRIEVE = "rag_retrieve";

    /**
     * 错误码：参数非法
     */
    public static final String ERROR_INVALID_PARAM = "INVALID_PARAM";

    /**
     * 错误码：服务内部错误
     */
    public static final String ERROR_INTERNAL = "INTERNAL_ERROR";

    /**
     * RAG 检索阶段：开始检索
     */
    public static final String RAG_START = "start";

    /**
     * RAG 检索阶段：检索命中
     */
    public static final String RAG_SUCCESS = "success";

    /**
     * RAG 检索阶段：未命中
     */
    public static final String RAG_EMPTY = "empty";

    /**
     * 检索知识拼接到系统提示词的前缀
     */
    public static final String KNOWLEDGE_PREFIX = "\n参考知识：\n";
}
