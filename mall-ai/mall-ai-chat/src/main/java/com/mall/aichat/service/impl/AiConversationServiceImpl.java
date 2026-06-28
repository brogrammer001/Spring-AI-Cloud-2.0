package com.mall.aichat.service.impl;

import com.mall.aichat.domain.AiConversation;
import com.mall.aichat.mapper.AiConversationMapper;
import com.mall.aichat.service.IAiConversationService;
import com.mall.common.core.utils.DateUtils;
import com.mall.common.core.utils.uuid.IdUtils;
import com.mall.common.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    @Resource(name = "mallRedisTemplate")
    private RedisTemplate<String, String> mallRedisTemplate;

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
    public boolean checkConversationOwner(Long userId, String conversationId) {
        String redisKey = "chat:conv:owner:" + conversationId;

        // 1. 先查 Redis
        String cachedUserId = mallRedisTemplate.opsForValue().get(redisKey);
        if (cachedUserId != null) {
            return String.valueOf(userId).equals(cachedUserId);
        }

        // 2. Redis 没有则查 DB
        Long dbUserId = aiConversationMapper.findByConversationId(conversationId);
        if (dbUserId == null) {
            return false; // 会话不存在
        }

        // 3. 回填 Redis
        mallRedisTemplate.opsForValue().set(redisKey, String.valueOf(dbUserId), 7, TimeUnit.DAYS);

        // 4. 比对
        return String.valueOf(userId).equals(dbUserId);
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

        String redisKey = "chat:conv:owner:" + entity.getConversationId();
        mallRedisTemplate.opsForValue().set(redisKey, String.valueOf(userId), 7, TimeUnit.DAYS);
        return entity;
    }
}
