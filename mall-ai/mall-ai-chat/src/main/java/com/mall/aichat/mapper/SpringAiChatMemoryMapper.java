package com.mall.aichat.mapper;

import com.mall.aichat.domain.SpringAiChatMemory;

import java.util.List;

/**
 * 【请填写功能名称】Mapper接口
 * 
 * @author mall
 * @date 2026-06-27
 */
public interface SpringAiChatMemoryMapper 
{
    /**
     * 查询【请填写功能名称】
     * 
     * @param conversationId 【请填写功能名称】主键
     * @return 【请填写功能名称】
     */
    public List<SpringAiChatMemory> selectSpringAiChatMemoryByConversationId(String conversationId);

    /**
     * 查询【请填写功能名称】列表
     * 
     * @param springAiChatMemory 【请填写功能名称】
     * @return 【请填写功能名称】集合
     */
    public List<SpringAiChatMemory> selectSpringAiChatMemoryList(SpringAiChatMemory springAiChatMemory);

    /**
     * 新增【请填写功能名称】
     * 
     * @param springAiChatMemory 【请填写功能名称】
     * @return 结果
     */
    public int insertSpringAiChatMemory(SpringAiChatMemory springAiChatMemory);

    /**
     * 修改【请填写功能名称】
     * 
     * @param springAiChatMemory 【请填写功能名称】
     * @return 结果
     */
    public int updateSpringAiChatMemory(SpringAiChatMemory springAiChatMemory);

    /**
     * 删除【请填写功能名称】
     * 
     * @param conversationId 【请填写功能名称】主键
     * @return 结果
     */
    public int deleteSpringAiChatMemoryByConversationId(String conversationId);

    /**
     * 批量删除【请填写功能名称】
     * 
     * @param conversationIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSpringAiChatMemoryByConversationIds(String[] conversationIds);
}
