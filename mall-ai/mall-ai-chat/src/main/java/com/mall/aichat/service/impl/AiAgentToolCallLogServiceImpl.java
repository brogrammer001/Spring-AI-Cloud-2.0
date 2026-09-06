package com.mall.aichat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.aichat.domain.AiAgentToolCallLog;
import com.mall.aichat.mapper.AiAgentToolCallLogMapper;
import com.mall.aichat.service.IAiAgentToolCallLogService;
import com.mall.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent工具调用审计日志Service业务层处理
 * 
 * @author mall
 * @date 2026-09-05
 */
@Service
public class AiAgentToolCallLogServiceImpl extends ServiceImpl<AiAgentToolCallLogMapper, AiAgentToolCallLog> implements IAiAgentToolCallLogService
{
    @Autowired
    private AiAgentToolCallLogMapper aiAgentToolCallLogMapper;

    /**
     * 查询Agent工具调用审计日志
     * 
     * @param callId Agent工具调用审计日志主键
     * @return Agent工具调用审计日志
     */
    @Override
    public AiAgentToolCallLog selectAiAgentToolCallLogByCallId(String callId)
    {
        return aiAgentToolCallLogMapper.selectAiAgentToolCallLogByCallId(callId);
    }

    /**
     * 查询Agent工具调用审计日志列表
     * 
     * @param aiAgentToolCallLog Agent工具调用审计日志
     * @return Agent工具调用审计日志
     */
    @Override
    public List<AiAgentToolCallLog> selectAiAgentToolCallLogList(AiAgentToolCallLog aiAgentToolCallLog)
    {
        return aiAgentToolCallLogMapper.selectAiAgentToolCallLogList(aiAgentToolCallLog);
    }

    /**
     * 新增Agent工具调用审计日志
     * 
     * @param aiAgentToolCallLog Agent工具调用审计日志
     * @return 结果
     */
    @Override
    public int insertAiAgentToolCallLog(AiAgentToolCallLog aiAgentToolCallLog)
    {
        aiAgentToolCallLog.setCreateTime(DateUtils.getNowDate());
        return aiAgentToolCallLogMapper.insertAiAgentToolCallLog(aiAgentToolCallLog);
    }

    /**
     * 修改Agent工具调用审计日志
     * 
     * @param aiAgentToolCallLog Agent工具调用审计日志
     * @return 结果
     */
    @Override
    public int updateAiAgentToolCallLog(AiAgentToolCallLog aiAgentToolCallLog)
    {
        aiAgentToolCallLog.setUpdateTime(DateUtils.getNowDate());
        return aiAgentToolCallLogMapper.updateAiAgentToolCallLog(aiAgentToolCallLog);
    }

    /**
     * 批量删除Agent工具调用审计日志
     * 
     * @param callIds 需要删除的Agent工具调用审计日志主键
     * @return 结果
     */
    @Override
    public int deleteAiAgentToolCallLogByCallIds(String[] callIds)
    {
        return aiAgentToolCallLogMapper.deleteAiAgentToolCallLogByCallIds(callIds);
    }

    @Override
    public int deleteAiAgentToolCallLogByConversationIds(String[] conversationIds)
    {
        return aiAgentToolCallLogMapper.deleteAiAgentToolCallLogByConversationIds(conversationIds);
    }

    /**
     * 删除Agent工具调用审计日志信息
     * 
     * @param callId Agent工具调用审计日志主键
     * @return 结果
     */
    @Override
    public int deleteAiAgentToolCallLogByCallId(String callId)
    {
        return aiAgentToolCallLogMapper.deleteAiAgentToolCallLogByCallId(callId);
    }
}
