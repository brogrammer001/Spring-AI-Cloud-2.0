package com.mall.aichat.service.impl;

import com.mall.aichat.domain.AiConversation;
import com.mall.aichat.mapper.AiConversationMapper;
import com.mall.aichat.service.IAiConversationService;
import com.mall.aichat.service.ISpringAiChatMemoryService;
import com.mall.aichat.service.ISysChatHistoryService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.exception.ServiceException;
import com.mall.common.core.utils.DateUtils;
import com.mall.common.core.utils.uuid.IdUtils;
import com.mall.common.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @author mall
 * @date 2026-06-27
 */
@Service
public class AiConversationServiceImpl implements IAiConversationService {

    @Autowired
    private AiConversationMapper aiConversationMapper;

    @Autowired
    private StringRedisTemplate mallRedisTemplate;

    @Autowired
    private ISpringAiChatMemoryService springAiChatMemoryService;

    @Autowired
    private ISysChatHistoryService sysChatHistoryService;

    @Resource(name = "conversationVectorStore")
    private VectorStore vectorStore;

    /**
     * 查询【请填写功能名称】
     *
     * @param id 【请填写功能名称】主键
     * @return 【请填写功能名称】
     */
    @Override
    public AiConversation selectAiConversationById(String id) {
        return aiConversationMapper.selectAiConversationById(id);
    }

    /**
     * 查询【请填写功能名称】列表
     *
     * @param aiConversation 【请填写功能名称】
     * @return 【请填写功能名称】
     */
    @Override
    public List<AiConversation> selectAiConversationList(AiConversation aiConversation) {
        return aiConversationMapper.selectAiConversationList(aiConversation);
    }

    /**
     * 新增【请填写功能名称】
     *
     * @param aiConversation 【请填写功能名称】
     * @return 结果
     */
    @Override
    public int insertAiConversation(AiConversation aiConversation) {
        aiConversation.setCreateTime(DateUtils.getNowDate());
        return aiConversationMapper.insertAiConversation(aiConversation);
    }

    /**
     * 修改【请填写功能名称】
     *
     * @param aiConversation 【请填写功能名称】
     * @return 结果
     */
    @Override
    public int updateAiConversation(AiConversation aiConversation) {
        aiConversation.setUpdateTime(DateUtils.getNowDate());
        return aiConversationMapper.updateAiConversation(aiConversation);
    }

    /**
     * 批量删除【请填写功能名称】
     *
     * @param ids 需要删除的【请填写功能名称】主键
     * @return 结果
     */
    @Override
    public int deleteAiConversationByIds(String[] ids) {
        return aiConversationMapper.deleteAiConversationByIds(ids);
    }

    /**
     * 删除【请填写功能名称】信息
     *
     * @param id 【请填写功能名称】主键
     * @return 结果
     */
    @Override
    public int deleteAiConversationById(String id) {
        return aiConversationMapper.deleteAiConversationById(id);
    }

    @Override
    public AiConversation createAiConversation(String question) {
        // 保存到数据库
        Long userId = SecurityUtils.getUserId();
        AiConversation entity = new AiConversation();
        entity.setId(IdUtils.fastUUID());
        entity.setUserId(userId);
        entity.setConversationId(IdUtils.fastUUID());
        if (question.length() <= 20) {
            entity.setTitle(question);
        } else {
            entity.setTitle(question.substring(0, 20) + "...");
        }
        this.insertAiConversation(entity);

        String redisKey = Constants.CHAT_CONVERSATION_KEY + entity.getConversationId();
        mallRedisTemplate.opsForValue().set(redisKey, String.valueOf(userId), 7, TimeUnit.DAYS);
        return entity;
    }

    @Override
    public int deleteByConversationId(String[] conversationIds) {
        //根据会话id获取关联表id
        String[] ids = Arrays.stream(conversationIds).flatMap(conversationId -> {
            AiConversation aiConversation = new AiConversation();
            aiConversation.setConversationId(conversationId);
            return this.selectAiConversationList(aiConversation).stream();
        }).map(AiConversation::getId).distinct().toArray(String[]::new);

        //删除用户与会话id关联
        int i = aiConversationMapper.deleteAiConversationByIds(ids);

        //删除上下文会话
        int j = springAiChatMemoryService.deleteSpringAiChatMemoryByConversationIds(conversationIds);

        //删除全量会话历史
        int z = sysChatHistoryService.deleteSysChatHistoryByConversationIds(conversationIds);

        //用户与会话id关联缓存
        List<String> chatConversationKey = Arrays.stream(conversationIds).map(conversationId -> Constants.CHAT_CONVERSATION_KEY + conversationId).collect(Collectors.toList());
        //会话内容缓存
        List<String> chatMemoryKey = Arrays.stream(conversationIds).map(conversationId -> Constants.CHAT_MEMORY_KEY + conversationId).toList();
        //保存全量会话新增顺序号缓存
        List<String> seqChatMemoryKey = Arrays.stream(conversationIds).map(conversationId -> Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId).toList();
        chatConversationKey.addAll(chatMemoryKey);
        chatConversationKey.addAll(seqChatMemoryKey);
        //删除redis缓存
        mallRedisTemplate.delete(chatConversationKey);

        //删除向量库会话
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(b.in("conversationId", conversationIds).build());

        if (i == 0 || j == 0 || z == 0) {
            throw new ServiceException("删除会话失败");
        }
        return i;
    }

}
