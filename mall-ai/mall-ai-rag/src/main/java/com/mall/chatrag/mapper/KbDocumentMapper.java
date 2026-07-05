package com.mall.chatrag.mapper;

import com.mall.chatrag.domain.KbDocument;

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
}
