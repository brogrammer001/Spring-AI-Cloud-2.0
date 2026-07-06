package com.mall.aichat.service.impl;

import com.mall.aichat.domain.KbKnowledgeBase;
import com.mall.aichat.mapper.KbKnowledgeBaseMapper;
import com.mall.aichat.service.IKbDocumentService;
import com.mall.aichat.service.IKbKnowledgeBaseService;
import com.mall.common.core.utils.DateUtils;
import com.mall.common.core.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库Service业务层处理
 *
 * @author mall
 * @date 2026-07-05
 */
@Service
public class KbKnowledgeBaseServiceImpl implements IKbKnowledgeBaseService {
    @Autowired
    private KbKnowledgeBaseMapper kbKnowledgeBaseMapper;
    @Autowired
    private IKbDocumentService kbDocumentService;

    /**
     * 查询知识库
     *
     * @param id 知识库主键
     * @return 知识库
     */
    @Override
    public KbKnowledgeBase selectKbKnowledgeBaseById(String id) {
        return kbKnowledgeBaseMapper.selectKbKnowledgeBaseById(id);
    }

    /**
     * 查询知识库列表
     *
     * @param kbKnowledgeBase 知识库
     * @return 知识库
     */
    @Override
    public List<KbKnowledgeBase> selectKbKnowledgeBaseList(KbKnowledgeBase kbKnowledgeBase) {
        return kbKnowledgeBaseMapper.selectKbKnowledgeBaseList(kbKnowledgeBase);
    }

    /**
     * 新增知识库
     *
     * @param kbKnowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int insertKbKnowledgeBase(KbKnowledgeBase kbKnowledgeBase) {
        kbKnowledgeBase.setCreateTime(DateUtils.getNowDate());
        kbKnowledgeBase.setId(IdUtils.fastUUID());
        return kbKnowledgeBaseMapper.insertKbKnowledgeBase(kbKnowledgeBase);
    }

    /**
     * 修改知识库
     *
     * @param kbKnowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int updateKbKnowledgeBase(KbKnowledgeBase kbKnowledgeBase) {
        kbKnowledgeBase.setUpdateTime(DateUtils.getNowDate());
        return kbKnowledgeBaseMapper.updateKbKnowledgeBase(kbKnowledgeBase);
    }

    /**
     * 批量删除知识库
     *
     * @param ids 需要删除的知识库主键
     * @return 结果
     */
    @Override
    public int deleteKbKnowledgeBaseByIds(String[] ids) {
        int i = kbKnowledgeBaseMapper.deleteKbKnowledgeBaseByIds(ids);

        kbDocumentService.deleteKbDocumentByKnowledgeIds(ids);
        return i;
    }

    /**
     * 删除知识库信息
     *
     * @param id 知识库主键
     * @return 结果
     */
    @Override
    public int deleteKbKnowledgeBaseById(String id) {
        return kbKnowledgeBaseMapper.deleteKbKnowledgeBaseById(id);
    }
}
