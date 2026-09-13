package com.mall.aichat.service.impl;


import com.mall.aichat.domain.Nl2sqlEval;
import com.mall.aichat.mapper.Nl2sqlEvalMapper;
import com.mall.aichat.service.INl2sqlEvalService;
import com.mall.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * NL2SQL黄金评测集Service业务层处理
 * 
 * @author mall
 * @date 2026-09-10
 */
@Service
public class Nl2sqlEvalServiceImpl implements INl2sqlEvalService 
{
    @Autowired
    private Nl2sqlEvalMapper nl2sqlEvalMapper;

    /**
     * 查询NL2SQL黄金评测集
     * 
     * @param id NL2SQL黄金评测集主键
     * @return NL2SQL黄金评测集
     */
    @Override
    public Nl2sqlEval selectNl2sqlEvalById(Long id)
    {
        return nl2sqlEvalMapper.selectNl2sqlEvalById(id);
    }

    /**
     * 查询NL2SQL黄金评测集列表
     * 
     * @param nl2sqlEval NL2SQL黄金评测集
     * @return NL2SQL黄金评测集
     */
    @Override
    public List<Nl2sqlEval> selectNl2sqlEvalList(Nl2sqlEval nl2sqlEval)
    {
        return nl2sqlEvalMapper.selectNl2sqlEvalList(nl2sqlEval);
    }

    /**
     * 新增NL2SQL黄金评测集
     * 
     * @param nl2sqlEval NL2SQL黄金评测集
     * @return 结果
     */
    @Override
    public int insertNl2sqlEval(Nl2sqlEval nl2sqlEval)
    {
        nl2sqlEval.setCreateTime(DateUtils.getNowDate());
        return nl2sqlEvalMapper.insertNl2sqlEval(nl2sqlEval);
    }

    /**
     * 修改NL2SQL黄金评测集
     * 
     * @param nl2sqlEval NL2SQL黄金评测集
     * @return 结果
     */
    @Override
    public int updateNl2sqlEval(Nl2sqlEval nl2sqlEval)
    {
        nl2sqlEval.setUpdateTime(DateUtils.getNowDate());
        return nl2sqlEvalMapper.updateNl2sqlEval(nl2sqlEval);
    }

    /**
     * 批量删除NL2SQL黄金评测集
     * 
     * @param ids 需要删除的NL2SQL黄金评测集主键
     * @return 结果
     */
    @Override
    public int deleteNl2sqlEvalByIds(Long[] ids)
    {
        return nl2sqlEvalMapper.deleteNl2sqlEvalByIds(ids);
    }

    /**
     * 删除NL2SQL黄金评测集信息
     * 
     * @param id NL2SQL黄金评测集主键
     * @return 结果
     */
    @Override
    public int deleteNl2sqlEvalById(Long id)
    {
        return nl2sqlEvalMapper.deleteNl2sqlEvalById(id);
    }
}
