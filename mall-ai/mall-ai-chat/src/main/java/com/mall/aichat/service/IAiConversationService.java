package com.mall.aichat.service;

import com.mall.aichat.domain.AiConversation;

import java.util.List;

/**
 * 【请填写功能名称】Service接口
 * 
 * @author mall
 * @date 2026-06-27
 */
public interface IAiConversationService 
{
    /**
     * 查询【请填写功能名称】
     * 
     * @param id 【请填写功能名称】主键
     * @return 【请填写功能名称】
     */
    public AiConversation selectAiConversationById(String id);

    /**
     * 查询【请填写功能名称】列表
     * 
     * @param aiConversation 【请填写功能名称】
     * @return 【请填写功能名称】集合
     */
    public List<AiConversation> selectAiConversationList(AiConversation aiConversation);

    /**
     * 新增【请填写功能名称】
     * 
     * @param aiConversation 【请填写功能名称】
     * @return 结果
     */
    public int insertAiConversation(AiConversation aiConversation);

    /**
     * 修改【请填写功能名称】
     * 
     * @param aiConversation 【请填写功能名称】
     * @return 结果
     */
    public int updateAiConversation(AiConversation aiConversation);

    /**
     * 批量删除【请填写功能名称】
     * 
     * @param ids 需要删除的【请填写功能名称】主键集合
     * @return 结果
     */
    public int deleteAiConversationByIds(String[] ids);

    /**
     * 删除【请填写功能名称】信息
     * 
     * @param id 【请填写功能名称】主键
     * @return 结果
     */
    public int deleteAiConversationById(String id);

    public boolean checkConversationOwner(Long userId, String conversationId);

    AiConversation createAiConversation(String question);
}
