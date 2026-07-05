package com.mall.chatrag.service.impl;

import com.mall.chatrag.domain.KbDocument;
import com.mall.chatrag.mapper.KbDocumentMapper;
import com.mall.chatrag.service.IKbDocumentService;
import com.mall.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库文档Service业务层处理
 * 
 * @author mall
 * @date 2026-07-05
 */
@Service
public class KbDocumentServiceImpl implements IKbDocumentService 
{
    @Autowired
    private KbDocumentMapper kbDocumentMapper;

    /**
     * 查询知识库文档
     * 
     * @param id 知识库文档主键
     * @return 知识库文档
     */
    @Override
    public KbDocument selectKbDocumentById(String id)
    {
        return kbDocumentMapper.selectKbDocumentById(id);
    }

    /**
     * 查询知识库文档列表
     * 
     * @param kbDocument 知识库文档
     * @return 知识库文档
     */
    @Override
    public List<KbDocument> selectKbDocumentList(KbDocument kbDocument)
    {
        return kbDocumentMapper.selectKbDocumentList(kbDocument);
    }

    /**
     * 新增知识库文档
     * 
     * @param kbDocument 知识库文档
     * @return 结果
     */
    @Override
    public int insertKbDocument(KbDocument kbDocument)
    {
        kbDocument.setCreateTime(DateUtils.getNowDate());
        return kbDocumentMapper.insertKbDocument(kbDocument);
    }

    /**
     * 修改知识库文档
     * 
     * @param kbDocument 知识库文档
     * @return 结果
     */
    @Override
    public int updateKbDocument(KbDocument kbDocument)
    {
        return kbDocumentMapper.updateKbDocument(kbDocument);
    }

    /**
     * 批量删除知识库文档
     * 
     * @param ids 需要删除的知识库文档主键
     * @return 结果
     */
    @Override
    public int deleteKbDocumentByIds(String[] ids)
    {
        return kbDocumentMapper.deleteKbDocumentByIds(ids);
    }

    /**
     * 删除知识库文档信息
     * 
     * @param id 知识库文档主键
     * @return 结果
     */
    @Override
    public int deleteKbDocumentById(String id)
    {
        return kbDocumentMapper.deleteKbDocumentById(id);
    }
}
