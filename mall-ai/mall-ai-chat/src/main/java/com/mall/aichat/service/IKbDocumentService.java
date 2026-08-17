package com.mall.aichat.service;

import com.mall.aichat.domain.KbDocument;

import java.util.List;

/**
 * 知识库文档Service接口
 * 
 * @author mall
 * @date 2026-07-05
 */
public interface IKbDocumentService 
{
    /**
     * 查询知识库文档
     * 
     * @param id 知识库文档主键
     * @return 知识库文档
     */
    public KbDocument selectKbDocumentById(String id);

    /**
     * 查询知识库文档列表
     * 
     * @param kbDocument 知识库文档
     * @return 知识库文档集合
     */
    public List<KbDocument> selectKbDocumentList(KbDocument kbDocument);

    /**
     * 新增知识库文档
     * 
     * @param kbDocument 知识库文档
     * @return 结果
     */
    public int insertKbDocument(KbDocument kbDocument);

    /**
     * 修改知识库文档
     * 
     * @param kbDocument 知识库文档
     * @return 结果
     */
    public int updateKbDocument(KbDocument kbDocument);

    /**
     * 批量删除知识库文档
     * 
     * @param ids 需要删除的知识库文档主键集合
     * @return 结果
     */
    public int deleteKbDocumentByIds(String[] ids);

    /**
     * 删除知识库文档信息
     * 
     * @param id 知识库文档主键
     * @return 结果
     */
    public int deleteKbDocumentById(String id);


    int deleteKbDocumentByKnowledgeIds(String[] ids);

    /**
     * 根据标签关键词查询匹配的文档列表（含 tags 和 knowledgeId，用于向量库双重过滤）
     *
     * @param tags   标签关键词（多个用逗号分隔）
     * @param kbType 知识库类型
     * @param status 状态
     * @return 匹配的文档列表
     */
    List<KbDocument> selectDocumentsByTags(String tags, String kbType, String status);

    /**
     * 根据知识库类型查询文档列表（两表关联查询）
     * <p>
     * 联合 kb_document 和 kb_knowledge_base 表一次查询，避免"查知识库+查文档"的两次查询开销
     *
     * @param kbType 知识库类型
     * @param status 知识库状态（0-启用）
     * @return 文档列表
     */
    List<KbDocument> selectDocumentsByKbType(String kbType, String status);
}
