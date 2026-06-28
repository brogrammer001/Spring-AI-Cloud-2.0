package com.mall.aichat.mapper;

import com.mall.aichat.domain.AiConversation;

import java.util.List;

/**
 * 【请填写功能名称】Mapper接口
 * 
 * @author mall
 * @date 2026-06-27
 */
public interface AiConversationMapper 
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
     * 删除【请填写功能名称】
     * 
     * @param id 【请填写功能名称】主键
     * @return 结果
     */
    public int deleteAiConversationById(String id);

    /**
     * 批量删除【请填写功能名称】
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiConversationByIds(String[] ids);

    Long findByConversationId(String conversationId);
}
