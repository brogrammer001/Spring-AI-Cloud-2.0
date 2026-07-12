package com.mall.aichat.advisor;

import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.utils.uuid.IdUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Date;
import java.util.List;

public class FullHistoryChatMemory implements ChatMemory {

    private final ChatMemory delegateWindowMemory; // 你的 MessageWindowChatMemory
    private final ISysChatHistoryService sysChatHistoryService;
    private final StringRedisTemplate stringRedisTemplate;


    public FullHistoryChatMemory(ChatMemory delegateWindowMemory, ISysChatHistoryService sysChatHistoryService, StringRedisTemplate stringRedisTemplate) {
        this.delegateWindowMemory = delegateWindowMemory;
        this.sysChatHistoryService = sysChatHistoryService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 批量添加同理
        delegateWindowMemory.add(conversationId, messages);

        List<SysChatHistory> sysChatHistories = messages.stream()
            .filter(m -> !(m instanceof ToolResponseMessage)
                && !(m instanceof AssistantMessage am && am.hasToolCalls()))
            .map(message -> {
                Long sequenceId = stringRedisTemplate.opsForValue().increment(Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId);
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
    }

    @Override
    public List<Message> get(String conversationId) {
        // 读取时，直接走窗口逻辑。AI 只需要看最近的 N 条。
        return delegateWindowMemory.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        // 清空时，建议两边都清
        //统一删除方法AiConversationServiceImpl.deleteByConversationId
    }
}
