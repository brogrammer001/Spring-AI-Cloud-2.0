package com.mall.chatrag.service.impl;

import com.mall.chatrag.domain.KbDocumentChunk;
import com.mall.chatrag.mapper.KbDocumentChunkMapper;
import com.mall.chatrag.service.IKbDocumentChunkService;
import com.mall.common.core.utils.DateUtils;
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
    public int deleteKbDocumentChunkByIds(String[] ids)
    {
        return kbDocumentChunkMapper.deleteKbDocumentChunkByIds(ids);
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
}
