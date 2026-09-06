package com.mall.aichat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.aichat.domain.AiAgentToolCallLog;

import java.util.List;

/**
 * Agent工具调用审计日志Service接口
 * 
 * @author mall
 * @date 2026-09-05
 */
public interface IAiAgentToolCallLogService extends IService<AiAgentToolCallLog>
{
    /**
     * 查询Agent工具调用审计日志
     * 
     * @param callId Agent工具调用审计日志主键
     * @return Agent工具调用审计日志
     */
    public AiAgentToolCallLog selectAiAgentToolCallLogByCallId(String callId);

    /**
     * 查询Agent工具调用审计日志列表
     * 
     * @param aiAgentToolCallLog Agent工具调用审计日志
     * @return Agent工具调用审计日志集合
     */
    public List<AiAgentToolCallLog> selectAiAgentToolCallLogList(AiAgentToolCallLog aiAgentToolCallLog);

    /**
     * 新增Agent工具调用审计日志
     * 
     * @param aiAgentToolCallLog Agent工具调用审计日志
     * @return 结果
     */
    public int insertAiAgentToolCallLog(AiAgentToolCallLog aiAgentToolCallLog);

    /**
     * 修改Agent工具调用审计日志
     * 
     * @param aiAgentToolCallLog Agent工具调用审计日志
     * @return 结果
     */
    public int updateAiAgentToolCallLog(AiAgentToolCallLog aiAgentToolCallLog);

    /**
     * 批量删除Agent工具调用审计日志
     * 
     * @param callIds 需要删除的Agent工具调用审计日志主键集合
     * @return 结果
     */
    public int deleteAiAgentToolCallLogByCallIds(String[] callIds);

    /**
     * 删除Agent工具调用审计日志信息
     * 
     * @param callId Agent工具调用审计日志主键
     * @return 结果
     */
    public int deleteAiAgentToolCallLogByCallId(String callId);

    int deleteAiAgentToolCallLogByConversationIds(String[] conversationIds);
}
