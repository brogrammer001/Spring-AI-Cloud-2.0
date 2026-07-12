package com.mall.aichat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.aichat.domain.SysChatHistory;

import java.util.List;

/**
 * 【请填写功能名称】Service接口
 * 
 * @author mall
 * @date 2026-07-04
 */
public interface ISysChatHistoryService extends IService<SysChatHistory>
{
    /**
     * 查询【请填写功能名称】
     * 
     * @param conversationId 【请填写功能名称】主键
     * @return 【请填写功能名称】
     */
    public List<SysChatHistory> selectSysChatHistoryByConversationId(String conversationId);

    public SysChatHistory selectSysChatHistoryById(String id);

    /**
     * 查询【请填写功能名称】列表
     * 
     * @param sysChatHistory 【请填写功能名称】
     * @return 【请填写功能名称】集合
     */
    public List<SysChatHistory> selectSysChatHistoryList(SysChatHistory sysChatHistory);

    /**
     * 新增【请填写功能名称】
     * 
     * @param sysChatHistory 【请填写功能名称】
     * @return 结果
     */
    public int insertSysChatHistory(SysChatHistory sysChatHistory);

    /**
     * 修改【请填写功能名称】
     * 
     * @param sysChatHistory 【请填写功能名称】
     * @return 结果
     */
    public int updateSysChatHistory(SysChatHistory sysChatHistory);

    /**
     * 批量删除【请填写功能名称】
     * 
     * @param conversationIds 需要删除的【请填写功能名称】主键集合
     * @return 结果
     */
    public int deleteSysChatHistoryByConversationIds(String[] conversationIds);

    /**
     * 删除【请填写功能名称】信息
     * 
     * @param conversationId 【请填写功能名称】主键
     * @return 结果
     */
    public int deleteSysChatHistoryByConversationId(String conversationId);

    public int deleteSysChatHistoryById(String id);

    List<SysChatHistory> selectSysChatHistoryListAll(SysChatHistory sysChatHistory);
}
