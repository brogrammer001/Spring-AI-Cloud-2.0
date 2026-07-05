package com.mall.chatrag.domain;

import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 知识库对象 kb_knowledge_base
 * 
 * @author mall
 * @date 2026-07-05
 */
public class KbKnowledgeBase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 知识库ID */
    private String id;

    /** 知识库名称 */
    @Excel(name = "知识库名称")
    private String name;

    /** 知识库描述 */
    @Excel(name = "知识库描述")
    private String description;

    /** 状态：1启用 0禁用 */
    @Excel(name = "状态：1启用 0禁用")
    private String status;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("description", getDescription())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
