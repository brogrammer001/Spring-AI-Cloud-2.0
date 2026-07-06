package com.mall.aichat.domain;

import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 知识库文档对象 kb_document
 * 
 * @author mall
 * @date 2026-07-05
 */
public class KbDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private String id;

    /** 所属知识库ID */
    @Excel(name = "所属知识库ID")
    private String knowledgeId;

    /** 文件名称 */
    @Excel(name = "文件名称")
    private String fileName;

    /** 文件存储路径 */
    @Excel(name = "文件存储路径")
    private String filePath;

    /** 文件类型(pdf, txt, md) */
    @Excel(name = "文件类型(pdf, txt, md)")
    private String fileType;

    /** 状态：0待处理 1处理中 2已完成 3失败 */
    @Excel(name = "状态：0待处理 1处理中 2已完成 3失败")
    private Long status;

    /** 切片数量 */
    @Excel(name = "切片数量")
    private Long chunkCount;

    @Excel(name = "分块大小")
    private Long chunkSize;

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setKnowledgeId(String knowledgeId) 
    {
        this.knowledgeId = knowledgeId;
    }

    public String getKnowledgeId() 
    {
        return knowledgeId;
    }

    public void setFileName(String fileName) 
    {
        this.fileName = fileName;
    }

    public String getFileName() 
    {
        return fileName;
    }

    public void setFilePath(String filePath) 
    {
        this.filePath = filePath;
    }

    public String getFilePath() 
    {
        return filePath;
    }

    public void setFileType(String fileType) 
    {
        this.fileType = fileType;
    }

    public String getFileType() 
    {
        return fileType;
    }

    public void setStatus(Long status) 
    {
        this.status = status;
    }

    public Long getStatus() 
    {
        return status;
    }

    public void setChunkCount(Long chunkCount) 
    {
        this.chunkCount = chunkCount;
    }

    public Long getChunkCount() 
    {
        return chunkCount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("knowledgeId", getKnowledgeId())
            .append("fileName", getFileName())
            .append("filePath", getFilePath())
            .append("fileType", getFileType())
            .append("status", getStatus())
            .append("chunkCount", getChunkCount())
            .append("createTime", getCreateTime())
            .toString();
    }
}
