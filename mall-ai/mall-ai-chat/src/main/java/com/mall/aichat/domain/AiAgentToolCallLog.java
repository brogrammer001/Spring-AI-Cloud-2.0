package com.mall.aichat.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;

/**
 * Agent工具调用审计日志对象 ai_agent_tool_call_log
 * 
 * @author mall
 * @date 2026-09-05
 */
public class AiAgentToolCallLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 单次调用ID（UUID） */
    private String callId;

    /** 本轮请求的追踪ID */
    @Excel(name = "本轮请求的追踪ID")
    private String traceId;

    /** 关联 ai_conversation.conversation_id */
    @Excel(name = "关联 ai_conversation.conversation_id")
    private String conversationId;

    /** 触发调用的用户 */
    @Excel(name = "触发调用的用户")
    private Long userId;

    /** 租户/部门隔离 */
    @Excel(name = "租户/部门隔离")
    private String tenantId;

    /** 工具名，如 createSupplier */
    @Excel(name = "工具名，如 createSupplier")
    private String toolName;

    /** 完整入参 */
    @Excel(name = "完整入参")
    private String toolParams;

    /** 结果摘要 */
    @Excel(name = "结果摘要")
    private String resultDigest;

    /** 大结果外置存储key，可空 */
    @Excel(name = "大结果外置存储key，可空")
    private String resultRef;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String status;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String errorMsg;

    /** 执行耗时(ms) */
    @Excel(name = "执行耗时(ms)")
    private Long costMs;

    /** 业务模块 */
    @Excel(name = "业务模块")
    private String bizModule;

    public void setCallId(String callId) 
    {
        this.callId = callId;
    }

    public String getCallId() 
    {
        return callId;
    }

    public void setTraceId(String traceId) 
    {
        this.traceId = traceId;
    }

    public String getTraceId() 
    {
        return traceId;
    }

    public void setConversationId(String conversationId) 
    {
        this.conversationId = conversationId;
    }

    public String getConversationId() 
    {
        return conversationId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setTenantId(String tenantId) 
    {
        this.tenantId = tenantId;
    }

    public String getTenantId() 
    {
        return tenantId;
    }

    public void setToolName(String toolName) 
    {
        this.toolName = toolName;
    }

    public String getToolName() 
    {
        return toolName;
    }

    public void setToolParams(String toolParams) 
    {
        this.toolParams = toolParams;
    }

    public String getToolParams() 
    {
        return toolParams;
    }

    public void setResultDigest(String resultDigest) 
    {
        this.resultDigest = resultDigest;
    }

    public String getResultDigest() 
    {
        return resultDigest;
    }

    public void setResultRef(String resultRef) 
    {
        this.resultRef = resultRef;
    }

    public String getResultRef() 
    {
        return resultRef;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setErrorMsg(String errorMsg) 
    {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() 
    {
        return errorMsg;
    }

    public void setCostMs(Long costMs) 
    {
        this.costMs = costMs;
    }

    public Long getCostMs() 
    {
        return costMs;
    }

    public void setBizModule(String bizModule) 
    {
        this.bizModule = bizModule;
    }

    public String getBizModule() 
    {
        return bizModule;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("callId", getCallId())
            .append("traceId", getTraceId())
            .append("conversationId", getConversationId())
            .append("userId", getUserId())
            .append("tenantId", getTenantId())
            .append("toolName", getToolName())
            .append("toolParams", getToolParams())
            .append("resultDigest", getResultDigest())
            .append("resultRef", getResultRef())
            .append("status", getStatus())
            .append("errorMsg", getErrorMsg())
            .append("costMs", getCostMs())
            .append("bizModule", getBizModule())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
