package com.mall.chatrag.domain;

import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 文档切片对象 kb_document_chunk
 * 
 * @author mall
 * @date 2026-07-05
 */
public class KbDocumentChunk extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 切片ID (与 Weaviate 中的 UUID 一致) */
    private String id;

    /** 所属文档ID */
    @Excel(name = "所属文档ID")
    private String documentId;

    /** 所属知识库ID */
    @Excel(name = "所属知识库ID")
    private String knowledgeId;

    /** 切片文本内容 */
    @Excel(name = "切片文本内容")
    private String content;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setDocumentId(String documentId) 
    {
        this.documentId = documentId;
    }

    public String getDocumentId() 
    {
        return documentId;
    }

    public void setKnowledgeId(String knowledgeId) 
    {
        this.knowledgeId = knowledgeId;
    }

    public String getKnowledgeId() 
    {
        return knowledgeId;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("documentId", getDocumentId())
            .append("knowledgeId", getKnowledgeId())
            .append("content", getContent())
            .append("createTime", getCreateTime())
            .toString();
    }
}
