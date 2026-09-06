package com.mall.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.aichat.domain.AiAgentToolCallLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent工具调用审计日志Mapper接口
 * 
 * @author mall
 * @date 2026-09-05
 */
public interface AiAgentToolCallLogMapper extends BaseMapper<AiAgentToolCallLog>
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
     * 删除Agent工具调用审计日志
     * 
     * @param callId Agent工具调用审计日志主键
     * @return 结果
     */
    public int deleteAiAgentToolCallLogByCallId(String callId);

    /**
     * 批量删除Agent工具调用审计日志
     * 
     * @param callIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiAgentToolCallLogByCallIds(String[] callIds);

    /**
     * 查询未处理的工具调用日志（记忆提炼 Worker 用）
     * <p>状态为 SUCCESS 且未被标记为已提炼的批量记录</p>
     *
     * @param limit 批量大小
     * @return 未处理的日志列表
     */
    List<AiAgentToolCallLog> selectUnprocessedBatch(@Param("limit") int limit);

    /**
     * 标记日志已处理（记忆提炼完成后回写）
     *
     * @param callIds 已处理的 callId 列表
     * @return 影响行数
     */
    int markProcessed(@Param("callIds") List<String> callIds);

    int deleteAiAgentToolCallLogByConversationIds(String[] conversationIds);
}
