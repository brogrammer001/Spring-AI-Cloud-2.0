package com.mall.aichat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.mapper.SysChatHistoryMapper;
import com.mall.aichat.service.ISysChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 【请填写功能名称】Service业务层处理
 * 
 * @author mall
 * @date 2026-07-04
 */
@Service
public class SysChatHistoryServiceImpl extends ServiceImpl<SysChatHistoryMapper, SysChatHistory> implements ISysChatHistoryService
{
    @Autowired
    private SysChatHistoryMapper sysChatHistoryMapper;

    /**
     * 查询【请填写功能名称】
     * 
     * @param conversationId 【请填写功能名称】主键
     * @return 【请填写功能名称】
     */
    @Override
    public List<SysChatHistory> selectSysChatHistoryByConversationId(String conversationId)
    {
        return sysChatHistoryMapper.selectSysChatHistoryByConversationId(conversationId);
    }

    /**
     * 查询【请填写功能名称】列表
     * 
     * @param sysChatHistory 【请填写功能名称】
     * @return 【请填写功能名称】
     */
    @Override
    public List<SysChatHistory> selectSysChatHistoryList(SysChatHistory sysChatHistory)
    {
        return sysChatHistoryMapper.selectSysChatHistoryList(sysChatHistory);
    }

    /**
     * 新增【请填写功能名称】
     * 
     * @param sysChatHistory 【请填写功能名称】
     * @return 结果
     */
    @Override
    public int insertSysChatHistory(SysChatHistory sysChatHistory)
    {
        return sysChatHistoryMapper.insertSysChatHistory(sysChatHistory);
    }

    /**
     * 修改【请填写功能名称】
     * 
     * @param sysChatHistory 【请填写功能名称】
     * @return 结果
     */
    @Override
    public int updateSysChatHistory(SysChatHistory sysChatHistory)
    {
        return sysChatHistoryMapper.updateSysChatHistory(sysChatHistory);
    }

    /**
     * 批量删除【请填写功能名称】
     * 
     * @param conversationIds 需要删除的【请填写功能名称】主键
     * @return 结果
     */
    @Override
    public int deleteSysChatHistoryByConversationIds(String[] conversationIds)
    {
        return sysChatHistoryMapper.deleteSysChatHistoryByConversationIds(conversationIds);
    }

    /**
     * 删除【请填写功能名称】信息
     * 
     * @param conversationId 【请填写功能名称】主键
     * @return 结果
     */
    @Override
    public int deleteSysChatHistoryByConversationId(String conversationId)
    {
        return sysChatHistoryMapper.deleteSysChatHistoryByConversationId(conversationId);
    }

    @Override
    public int deleteSysChatHistoryById(String id) {
        return sysChatHistoryMapper.deleteSysChatHistoryById(id);
    }
}
