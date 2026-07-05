package com.mall.chatrag.mapper;

import com.mall.chatrag.domain.KbKnowledgeBase;

import java.util.List;

/**
 * 知识库Mapper接口
 * 
 * @author mall
 * @date 2026-07-05
 */
public interface KbKnowledgeBaseMapper 
{
    /**
     * 查询知识库
     * 
     * @param id 知识库主键
     * @return 知识库
     */
    public KbKnowledgeBase selectKbKnowledgeBaseById(String id);

    /**
     * 查询知识库列表
     * 
     * @param kbKnowledgeBase 知识库
     * @return 知识库集合
     */
    public List<KbKnowledgeBase> selectKbKnowledgeBaseList(KbKnowledgeBase kbKnowledgeBase);

    /**
     * 新增知识库
     * 
     * @param kbKnowledgeBase 知识库
     * @return 结果
     */
    public int insertKbKnowledgeBase(KbKnowledgeBase kbKnowledgeBase);

    /**
     * 修改知识库
     * 
     * @param kbKnowledgeBase 知识库
     * @return 结果
     */
    public int updateKbKnowledgeBase(KbKnowledgeBase kbKnowledgeBase);

    /**
     * 删除知识库
     * 
     * @param id 知识库主键
     * @return 结果
     */
    public int deleteKbKnowledgeBaseById(String id);

    /**
     * 批量删除知识库
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteKbKnowledgeBaseByIds(String[] ids);
}
