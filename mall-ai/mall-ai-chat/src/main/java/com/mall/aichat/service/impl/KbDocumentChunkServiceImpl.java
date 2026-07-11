package com.mall.aichat.service.impl;

import com.mall.aichat.domain.KbDocumentChunk;
import com.mall.aichat.mapper.KbDocumentChunkMapper;
import com.mall.aichat.service.IKbDocumentChunkService;
import com.mall.common.core.utils.DateUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档切片Service业务层处理
 * 
 * @author mall
 * @date 2026-07-05
 */
@Service
public class KbDocumentChunkServiceImpl implements IKbDocumentChunkService 
{
    @Autowired
    private KbDocumentChunkMapper kbDocumentChunkMapper;

    @Resource(name = "knowledgeVectorStore")
    private VectorStore vectorStore;

    /**
     * 查询文档切片
     * 
     * @param id 文档切片主键
     * @return 文档切片
     */
    @Override
    public KbDocumentChunk selectKbDocumentChunkById(String id)
    {
        return kbDocumentChunkMapper.selectKbDocumentChunkById(id);
    }

    /**
     * 查询文档切片列表
     * 
     * @param kbDocumentChunk 文档切片
     * @return 文档切片
     */
    @Override
    public List<KbDocumentChunk> selectKbDocumentChunkList(KbDocumentChunk kbDocumentChunk)
    {
        return kbDocumentChunkMapper.selectKbDocumentChunkList(kbDocumentChunk);
    }

    /**
     * 新增文档切片
     * 
     * @param kbDocumentChunk 文档切片
     * @return 结果
     */
    @Override
    public int insertKbDocumentChunk(KbDocumentChunk kbDocumentChunk)
    {
        kbDocumentChunk.setCreateTime(DateUtils.getNowDate());
        return kbDocumentChunkMapper.insertKbDocumentChunk(kbDocumentChunk);
    }

    /**
     * 修改文档切片
     * 
     * @param kbDocumentChunk 文档切片
     * @return 结果
     */
    @Override
    public int updateKbDocumentChunk(KbDocumentChunk kbDocumentChunk)
    {
        return kbDocumentChunkMapper.updateKbDocumentChunk(kbDocumentChunk);
    }

    /**
     * 批量删除文档切片
     * 
     * @param ids 需要删除的文档切片主键
     * @return 结果
     */
    @Override
    public int deleteKbDocumentChunkByIds(String[] ids) {
        int i = kbDocumentChunkMapper.deleteKbDocumentChunkByIds(ids);

        vectorStore.delete(List.of(ids));
        return i;
    }

    /**
     * 删除文档切片信息
     * 
     * @param id 文档切片主键
     * @return 结果
     */
    @Override
    public int deleteKbDocumentChunkById(String id)
    {
        return kbDocumentChunkMapper.deleteKbDocumentChunkById(id);
    }

    @Override
    public int deleteKbDocumentChunkByKnowledgeIds(String[] knowledgeIds) {
        int i = kbDocumentChunkMapper.deleteKbDocumentChunkByKnowledgeIds(knowledgeIds);

        FilterExpressionBuilder b = new FilterExpressionBuilder();

        // 构建过滤条件：meta_knowledge_id == knowledgeId
        vectorStore.delete(b.in("knowledgeId", knowledgeIds).build());
        return i;
    }
}
