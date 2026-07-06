package com.mall.aichat.mapper;

import com.mall.aichat.domain.KbDocumentChunk;

import java.util.List;

/**
 * 文档切片Mapper接口
 * 
 * @author mall
 * @date 2026-07-05
 */
public interface KbDocumentChunkMapper 
{
    /**
     * 查询文档切片
     * 
     * @param id 文档切片主键
     * @return 文档切片
     */
    public KbDocumentChunk selectKbDocumentChunkById(String id);

    /**
     * 查询文档切片列表
     * 
     * @param kbDocumentChunk 文档切片
     * @return 文档切片集合
     */
    public List<KbDocumentChunk> selectKbDocumentChunkList(KbDocumentChunk kbDocumentChunk);

    /**
     * 新增文档切片
     * 
     * @param kbDocumentChunk 文档切片
     * @return 结果
     */
    public int insertKbDocumentChunk(KbDocumentChunk kbDocumentChunk);

    /**
     * 修改文档切片
     * 
     * @param kbDocumentChunk 文档切片
     * @return 结果
     */
    public int updateKbDocumentChunk(KbDocumentChunk kbDocumentChunk);

    /**
     * 删除文档切片
     * 
     * @param id 文档切片主键
     * @return 结果
     */
    public int deleteKbDocumentChunkById(String id);

    /**
     * 批量删除文档切片
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteKbDocumentChunkByIds(String[] ids);

    int deleteKbDocumentChunkByKnowledgeIds(String[] ids);
}
