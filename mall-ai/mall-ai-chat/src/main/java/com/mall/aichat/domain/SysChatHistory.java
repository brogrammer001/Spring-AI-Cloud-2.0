package com.mall.aichat.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;

/**
 * 【请填写功能名称】对象 sys_chat_history
 * 
 * @author mall
 * @date 2026-07-04
 */
public class SysChatHistory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    @Excel(name = "会话ID")
    private String conversationId;

    /** 消息内容 */
    @Excel(name = "消息内容")
    private String content;

    /** 消息类型：USER / ASSISTANT */
    @Excel(name = "消息类型：USER / ASSISTANT")
    private String type;

    /** 时间戳 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "时间戳", width = 30, dateFormat = "yyyy-MM-dd")
    private Date timestamp;

    /** 排序 */
    @Excel(name = "排序")
    private Long sequenceId;

    public void setConversationId(String conversationId) 
    {
        this.conversationId = conversationId;
    }

    public String getConversationId() 
    {
        return conversationId;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setTimestamp(Date timestamp) 
    {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() 
    {
        return timestamp;
    }

    public void setSequenceId(Long sequenceId) 
    {
        this.sequenceId = sequenceId;
    }

    public Long getSequenceId() 
    {
        return sequenceId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("conversationId", getConversationId())
            .append("content", getContent())
            .append("type", getType())
            .append("timestamp", getTimestamp())
            .append("sequenceId", getSequenceId())
            .toString();
    }
}
