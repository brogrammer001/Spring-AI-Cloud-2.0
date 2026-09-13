package com.mall.aichat.service;

import com.mall.aichat.domain.Nl2sqlEval;

import java.util.List;

/**
 * NL2SQL黄金评测集Service接口
 * 
 * @author mall
 * @date 2026-09-10
 */
public interface INl2sqlEvalService 
{
    /**
     * 查询NL2SQL黄金评测集
     * 
     * @param id NL2SQL黄金评测集主键
     * @return NL2SQL黄金评测集
     */
    public Nl2sqlEval selectNl2sqlEvalById(Long id);

    /**
     * 查询NL2SQL黄金评测集列表
     * 
     * @param nl2sqlEval NL2SQL黄金评测集
     * @return NL2SQL黄金评测集集合
     */
    public List<Nl2sqlEval> selectNl2sqlEvalList(Nl2sqlEval nl2sqlEval);

    /**
     * 新增NL2SQL黄金评测集
     * 
     * @param nl2sqlEval NL2SQL黄金评测集
     * @return 结果
     */
    public int insertNl2sqlEval(Nl2sqlEval nl2sqlEval);

    /**
     * 修改NL2SQL黄金评测集
     * 
     * @param nl2sqlEval NL2SQL黄金评测集
     * @return 结果
     */
    public int updateNl2sqlEval(Nl2sqlEval nl2sqlEval);

    /**
     * 批量删除NL2SQL黄金评测集
     * 
     * @param ids 需要删除的NL2SQL黄金评测集主键集合
     * @return 结果
     */
    public int deleteNl2sqlEvalByIds(Long[] ids);

    /**
     * 删除NL2SQL黄金评测集信息
     * 
     * @param id NL2SQL黄金评测集主键
     * @return 结果
     */
    public int deleteNl2sqlEvalById(Long id);
}
