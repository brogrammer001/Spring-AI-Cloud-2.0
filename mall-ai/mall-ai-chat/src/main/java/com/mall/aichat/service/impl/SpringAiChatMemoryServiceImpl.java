package com.mall.aichat.service.impl;

import com.mall.aichat.domain.SpringAiChatMemory;
import com.mall.aichat.mapper.SpringAiChatMemoryMapper;
import com.mall.aichat.service.ISpringAiChatMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 【请填写功能名称】Service业务层处理
 * 
 * @author mall
 * @date 2026-06-27
 */
@Service
public class SpringAiChatMemoryServiceImpl implements ISpringAiChatMemoryService 
{
    @Autowired
    private SpringAiChatMemoryMapper springAiChatMemoryMapper;

    /**
     * 查询【请填写功能名称】
     * 
     * @param conversationId 【请填写功能名称】主键
     * @return 【请填写功能名称】
     */
    @Override
    public List<SpringAiChatMemory> selectSpringAiChatMemoryByConversationId(String conversationId)
    {
        return springAiChatMemoryMapper.selectSpringAiChatMemoryByConversationId(conversationId);
    }

    /**
     * 查询【请填写功能名称】列表
     * 
     * @param springAiChatMemory 【请填写功能名称】
     * @return 【请填写功能名称】
     */
    @Override
    public List<SpringAiChatMemory> selectSpringAiChatMemoryList(SpringAiChatMemory springAiChatMemory)
    {
        return springAiChatMemoryMapper.selectSpringAiChatMemoryList(springAiChatMemory);
    }

    /**
     * 新增【请填写功能名称】
     * 
     * @param springAiChatMemory 【请填写功能名称】
     * @return 结果
     */
    @Override
    public int insertSpringAiChatMemory(SpringAiChatMemory springAiChatMemory)
    {
        return springAiChatMemoryMapper.insertSpringAiChatMemory(springAiChatMemory);
    }

    /**
     * 修改【请填写功能名称】
     * 
     * @param springAiChatMemory 【请填写功能名称】
     * @return 结果
     */
    @Override
    public int updateSpringAiChatMemory(SpringAiChatMemory springAiChatMemory)
    {
        return springAiChatMemoryMapper.updateSpringAiChatMemory(springAiChatMemory);
    }

    /**
     * 批量删除【请填写功能名称】
     * 
     * @param conversationIds 需要删除的【请填写功能名称】主键
     * @return 结果
     */
    @Override
    public int deleteSpringAiChatMemoryByConversationIds(String[] conversationIds)
    {
        return springAiChatMemoryMapper.deleteSpringAiChatMemoryByConversationIds(conversationIds);
    }

    /**
     * 删除【请填写功能名称】信息
     * 
     * @param conversationId 【请填写功能名称】主键
     * @return 结果
     */
    @Override
    public int deleteSpringAiChatMemoryByConversationId(String conversationId)
    {
        return springAiChatMemoryMapper.deleteSpringAiChatMemoryByConversationId(conversationId);
    }
}
