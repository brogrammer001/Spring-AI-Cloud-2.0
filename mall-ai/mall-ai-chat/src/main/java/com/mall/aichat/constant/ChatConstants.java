package com.mall.aichat.constant;

/**
 * AI 对话流式交互常量信息
 *
 * @author mall
 */
public class ChatConstants
{

    // =====================================================================
    // ① 会话记忆库（chatMemory）—— VectorStoreChatMemoryAdvisor 读写
    // =====================================================================

    /** 归属会话 ID，advisor 检索/压缩合并的基础过滤字段 */
    public static final String CHAT_MEMORY_CONVERSATION_ID = "conversationId";
    /** 消息角色（USER / ASSISTANT / SYSTEM），AdvisorMessage.MessageType.name() */
    public static final String CHAT_MEMORY_MESSAGE_TYPE = "messageType";
    /**
     * 记忆状态枚举值（Schema 用 text 存，避免 Weaviate boolean 过滤问题）
     */
    public static final String STATUS_ACTIVE = "active";
    /** 记忆状态：active / archived / superseded / expired（用字符串而非 boolean） */
    public static final String CHAT_MEMORY_STATUS = "status";
    /** 记忆写入时间戳（毫秒），用于排序与增量同步 */
    public static final String CHAT_MEMORY_INGESTED_AT = "ingestedAt";
    /** 记忆过期时间戳（毫秒），0 表示不过期；配合定时任务或过滤表达式实现 TTL */
    public static final String CHAT_MEMORY_EXPIRE_AT = "expireAt";
    /** 记忆归属用户 ID，长期记忆跨会话检索的基础过滤字段（conversationId 仅随 metadata 落库用于追踪） */
    public static final String CHAT_MEMORY_USER_ID = "userId";

    // =====================================================================
    // ② 知识库—— RAG 写入方 / 检索方读写
    // =====================================================================

    /** 知识库归属 ID，RAG 检索/删除的基础过滤字段 */
    public static final String KNOWLEDGE_ID = "knowledgeId";
    /** 文档来源标识（如 URL / 上传渠道） */
    public static final String KNOWLEDGE_SOURCE = "source";
    /** 文档文件名 */
    public static final String KNOWLEDGE_FILENAME = "filename";

    // =====================================================================
    // ③ 工具索引库—— VectorToolIndex 框架读写
    // =====================================================================

    /** 工具注册会话 ID。框架级字段：索引时写入、检索时按会话过滤，不可删除。
     *  未来若引入「全局工具」，约定全局工具写空串 ""，检索用 OR 放行 */
    public static final String TOOL_SESSION_ID = "sessionId";
    /** 工具名，便于按名过滤 */
    public static final String TOOL_NAME = "toolName";

    // =====================================================================
    // Advisor context key（非 Document metadata，但同属跨模块契约，一并收敛）
    // =====================================================================

    /** 请求级覆盖记忆检索条数的 context key */
    public static final String CTX_CHAT_MEMORY_TOP_K = "chat_memory_vector_store_top_k";

    /** 请求级用户标识 context key，VectorStoreChatMemoryAdvisor 据此圈定长期记忆作用域（跨会话生效） */
    public static final String CTX_USER_ID = "chat_memory_user_id";

    /** 请求级指定知识库类型（kbType）的 context key，RagContextQueryAdvisor 据此检索知识库 */
    public static final String CTX_KB_TYPE = "rag_kb_type";

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
