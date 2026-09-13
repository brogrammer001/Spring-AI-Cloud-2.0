package com.mall.aichat.mapper;

import com.mall.aichat.domain.Nl2sqlBusinessRule;

import java.util.List;

/**
 * NL2SQL业务规则Mapper接口
 * 
 * @author mall
 * @date 2026-09-10
 */
public interface Nl2sqlBusinessRuleMapper 
{
    /**
     * 查询NL2SQL业务规则
     * 
     * @param id NL2SQL业务规则主键
     * @return NL2SQL业务规则
     */
    public Nl2sqlBusinessRule selectNl2sqlBusinessRuleById(Long id);

    /**
     * 查询NL2SQL业务规则列表
     * 
     * @param nl2sqlBusinessRule NL2SQL业务规则
     * @return NL2SQL业务规则集合
     */
    public List<Nl2sqlBusinessRule> selectNl2sqlBusinessRuleList(Nl2sqlBusinessRule nl2sqlBusinessRule);

    /**
     * 新增NL2SQL业务规则
     * 
     * @param nl2sqlBusinessRule NL2SQL业务规则
     * @return 结果
     */
    public int insertNl2sqlBusinessRule(Nl2sqlBusinessRule nl2sqlBusinessRule);

    /**
     * 修改NL2SQL业务规则
     * 
     * @param nl2sqlBusinessRule NL2SQL业务规则
     * @return 结果
     */
    public int updateNl2sqlBusinessRule(Nl2sqlBusinessRule nl2sqlBusinessRule);

    /**
     * 删除NL2SQL业务规则
     * 
     * @param id NL2SQL业务规则主键
     * @return 结果
     */
    public int deleteNl2sqlBusinessRuleById(Long id);

    /**
     * 批量删除NL2SQL业务规则
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteNl2sqlBusinessRuleByIds(Long[] ids);
}
