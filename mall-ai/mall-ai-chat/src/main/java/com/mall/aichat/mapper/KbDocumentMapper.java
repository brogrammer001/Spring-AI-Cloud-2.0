package com.mall.aichat.mapper;

import com.mall.aichat.domain.KbDocument;

import java.util.List;

/**
 * 知识库文档Mapper接口
 * 
 * @author mall
 * @date 2026-07-05
 */
public interface KbDocumentMapper 
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
     * 删除知识库文档
     * 
     * @param id 知识库文档主键
     * @return 结果
     */
    public int deleteKbDocumentById(String id);

    /**
     * 批量删除知识库文档
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteKbDocumentByIds(String[] ids);

    int deleteKbDocumentByKnowledgeIds(String[] ids);

    /**
     * 根据标签关键词查询匹配的文档列表（返回tags和knowledgeId，用于向量库双重过滤）
     * <p>
     * 用于RAG检索的第一步：先按标签匹配文档，获取精确的tags值和knowledgeId，
     * 再在向量检索时同时按 tags 和 knowledgeId 过滤，提升检索精度
     *
     * @param tags    标签关键词（多个用逗号分隔）
     * @param kbType  知识库类型
     * @param status  状态（0-启用）
     * @return 匹配的文档列表（含 tags 和 knowledgeId 字段）
     */
    List<KbDocument> selectDocumentsByTags(@org.apache.ibatis.annotations.Param("tags") String tags,
                                            @org.apache.ibatis.annotations.Param("kbType") String kbType,
                                            @org.apache.ibatis.annotations.Param("status") String status);

    /**
     * 根据知识库类型查询文档列表（两表关联查询）
     * <p>
     * 联合 kb_document 和 kb_knowledge_base 表，一次查询获取指定 kbType 下所有启用的文档，
     * 避免先查知识库再查文档的两次查询开销。
     *
     * @param kbType 知识库类型
     * @param status 知识库状态（0-启用）
     * @return 文档列表
     */
    List<KbDocument> selectDocumentsByKbType(@org.apache.ibatis.annotations.Param("kbType") String kbType,
                                              @org.apache.ibatis.annotations.Param("status") String status);
}
