package com.mall.aichat.service.impl;


import com.mall.aichat.domain.Nl2sqlBusinessRule;
import com.mall.aichat.mapper.Nl2sqlBusinessRuleMapper;
import com.mall.aichat.service.INl2sqlBusinessRuleService;
import com.mall.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * NL2SQL业务规则Service业务层处理
 * 
 * @author mall
 * @date 2026-09-10
 */
@Service
public class Nl2sqlBusinessRuleServiceImpl implements INl2sqlBusinessRuleService 
{
    @Autowired
    private Nl2sqlBusinessRuleMapper nl2sqlBusinessRuleMapper;

    /**
     * 查询NL2SQL业务规则
     * 
     * @param id NL2SQL业务规则主键
     * @return NL2SQL业务规则
     */
    @Override
    public Nl2sqlBusinessRule selectNl2sqlBusinessRuleById(Long id)
    {
        return nl2sqlBusinessRuleMapper.selectNl2sqlBusinessRuleById(id);
    }

    /**
     * 查询NL2SQL业务规则列表
     * 
     * @param nl2sqlBusinessRule NL2SQL业务规则
     * @return NL2SQL业务规则
     */
    @Override
    public List<Nl2sqlBusinessRule> selectNl2sqlBusinessRuleList(Nl2sqlBusinessRule nl2sqlBusinessRule)
    {
        return nl2sqlBusinessRuleMapper.selectNl2sqlBusinessRuleList(nl2sqlBusinessRule);
    }

    /**
     * 新增NL2SQL业务规则
     * 
     * @param nl2sqlBusinessRule NL2SQL业务规则
     * @return 结果
     */
    @Override
    public int insertNl2sqlBusinessRule(Nl2sqlBusinessRule nl2sqlBusinessRule)
    {
        nl2sqlBusinessRule.setCreateTime(DateUtils.getNowDate());
        return nl2sqlBusinessRuleMapper.insertNl2sqlBusinessRule(nl2sqlBusinessRule);
    }

    /**
     * 修改NL2SQL业务规则
     * 
     * @param nl2sqlBusinessRule NL2SQL业务规则
     * @return 结果
     */
    @Override
    public int updateNl2sqlBusinessRule(Nl2sqlBusinessRule nl2sqlBusinessRule)
    {
        nl2sqlBusinessRule.setUpdateTime(DateUtils.getNowDate());
        return nl2sqlBusinessRuleMapper.updateNl2sqlBusinessRule(nl2sqlBusinessRule);
    }

    /**
     * 批量删除NL2SQL业务规则
     * 
     * @param ids 需要删除的NL2SQL业务规则主键
     * @return 结果
     */
    @Override
    public int deleteNl2sqlBusinessRuleByIds(Long[] ids)
    {
        return nl2sqlBusinessRuleMapper.deleteNl2sqlBusinessRuleByIds(ids);
    }

    /**
     * 删除NL2SQL业务规则信息
     * 
     * @param id NL2SQL业务规则主键
     * @return 结果
     */
    @Override
    public int deleteNl2sqlBusinessRuleById(Long id)
    {
        return nl2sqlBusinessRuleMapper.deleteNl2sqlBusinessRuleById(id);
    }
}
