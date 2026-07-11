package com.mall.aichat.advisor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.utils.StringUtils;
import com.mall.common.core.utils.uuid.IdUtils;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 存储聊天会话
 * 根据maxMessages配置最多存储多少条对话
 */
public class RedisCachedAndMysqlMemoryRepository implements ChatMemoryRepository {
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;
    private final StringRedisTemplate mallRedisTemplate;
    private final ISysChatHistoryService sysChatHistoryService;
    public static final long TTL_DAYS = 7;  // Redis 热缓存保留 7 天

    public RedisCachedAndMysqlMemoryRepository(JdbcChatMemoryRepository jdbcChatMemoryRepository, StringRedisTemplate mallRedisTemplate,
                                               ISysChatHistoryService sysChatHistoryService) {
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
        this.mallRedisTemplate = mallRedisTemplate;
        this.sysChatHistoryService = sysChatHistoryService;
    }

    @Override
    public List<String> findConversationIds() {
        return jdbcChatMemoryRepository.findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = Constants.CHAT_MEMORY_KEY + conversationId;
        try {
            // 1. 先查 Redis
            String cached = mallRedisTemplate.opsForValue().get(key);
            if (StringUtils.isNotEmpty(cached)) {
                return deserialize(cached);
            }

            // 2. 未命中查 DB，并回填 Redis
            List<Message> msgs = jdbcChatMemoryRepository.findByConversationId(conversationId);
            mallRedisTemplate.opsForValue().set(key, serialize(msgs), TTL_DAYS, TimeUnit.DAYS);
            return msgs;
        } catch (Exception e) {
            // 建议降级：如果 Redis 挂了，直接查库，不影响主流程
            return jdbcChatMemoryRepository.findByConversationId(conversationId);
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            // 1. 先落库
            jdbcChatMemoryRepository.saveAll(conversationId, messages);
            // 2. 更新缓存
            mallRedisTemplate.opsForValue().set(Constants.CHAT_MEMORY_KEY + conversationId,
                serialize(messages), TTL_DAYS, TimeUnit.DAYS);

            List<SysChatHistory> sysChatHistories = messages.stream()
                .filter(m -> !(m instanceof ToolResponseMessage)
                    && !(m instanceof AssistantMessage am && am.hasToolCalls()))
                .map(message -> {
                    Long sequenceId = mallRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId);
                    SysChatHistory sysChatHistory = new SysChatHistory();
                    sysChatHistory.setId(IdUtils.fastUUID());
                    sysChatHistory.setConversationId(conversationId);
                    sysChatHistory.setContent(message.getText());
                    sysChatHistory.setTimestamp(new Date());
                    sysChatHistory.setIsCompression("N");
                    sysChatHistory.setType(message.getMessageType().getValue());
                    sysChatHistory.setSequenceId(sequenceId);
                    return sysChatHistory;
                })
                .toList();

            if (sysChatHistories.isEmpty()) {
                return;
            }
            sysChatHistoryService.saveBatch(sysChatHistories);
        } catch (Exception e) {
            // 缓存更新失败不影响主流程，可打印日志
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        //统一删除方法AiConversationServiceImpl.deleteByConversationId
    }

    // ==========================================
    // 核心优化：序列化与反序列化方法
    // ==========================================

    private String serialize(List<Message> messages) {
        // Feature.WriteClassName: 关键特性！
        // 它会在 JSON 中自动写入 "@type":"org.springframework.ai.chat.messages.UserMessage"
        // 这样反序列化时就能自动还原为正确的具体类型
        return JSON.toJSONString(messages, JSONWriter.Feature.WriteClassName);
    }

    /**
     * 优化后的反序列化方法：手动解析，确保通过构造函数创建对象
     */
    private List<Message> deserialize(String json) {
        JSONArray jsonArray = JSON.parseArray(json);
        List<Message> messages = new ArrayList<>(jsonArray.size());

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObj = jsonArray.getJSONObject(i);

            // 1. 获取消息内容 (兼容 'content' 和 'text' 两种可能的字段名)
            String content = jsonObj.getString("content");
            if (content == null) {
                content = jsonObj.getString("text");
            }

            // 2. 确定消息类型
            // 优先读取 Fastjson2 写入的 @type 字段
            String typeStr = jsonObj.getString("@type");
            if (typeStr == null) {
                // 兼容旧数据，读取 messageType 字段
                typeStr = jsonObj.getString("messageType");
            }

            // 3. 根据类型创建具体的 Message 对象
            // 注意：必须使用构造函数创建，不能只用 FieldBased 赋值
            Message message = null;
            if (typeStr != null) {
                if (typeStr.contains("UserMessage") || "USER".equalsIgnoreCase(typeStr)) {
                    message = new UserMessage(content);
                } else if (typeStr.contains("AssistantMessage") || "ASSISTANT".equalsIgnoreCase(typeStr)) {
                    message = new AssistantMessage(content);
                } else if (typeStr.contains("SystemMessage") || "SYSTEM".equalsIgnoreCase(typeStr)) {
                    message = new SystemMessage(content);
                }
            }

            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }
}
