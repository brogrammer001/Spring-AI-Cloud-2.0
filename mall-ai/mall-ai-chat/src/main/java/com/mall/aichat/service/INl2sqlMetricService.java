package com.mall.aichat.service;

import com.mall.aichat.domain.Nl2sqlMetric;

import java.util.List;

/**
 * NL2SQL指标定义Service接口
 * 
 * @author mall
 * @date 2026-09-10
 */
public interface INl2sqlMetricService 
{
    /**
     * 查询NL2SQL指标定义
     * 
     * @param id NL2SQL指标定义主键
     * @return NL2SQL指标定义
     */
    public Nl2sqlMetric selectNl2sqlMetricById(Long id);

    /**
     * 查询NL2SQL指标定义列表
     * 
     * @param nl2sqlMetric NL2SQL指标定义
     * @return NL2SQL指标定义集合
     */
    public List<Nl2sqlMetric> selectNl2sqlMetricList(Nl2sqlMetric nl2sqlMetric);

    /**
     * 新增NL2SQL指标定义
     * 
     * @param nl2sqlMetric NL2SQL指标定义
     * @return 结果
     */
    public int insertNl2sqlMetric(Nl2sqlMetric nl2sqlMetric);

    /**
     * 修改NL2SQL指标定义
     * 
     * @param nl2sqlMetric NL2SQL指标定义
     * @return 结果
     */
    public int updateNl2sqlMetric(Nl2sqlMetric nl2sqlMetric);

    /**
     * 批量删除NL2SQL指标定义
     * 
     * @param ids 需要删除的NL2SQL指标定义主键集合
     * @return 结果
     */
    public int deleteNl2sqlMetricByIds(Long[] ids);

    /**
     * 删除NL2SQL指标定义信息
     * 
     * @param id NL2SQL指标定义主键
     * @return 结果
     */
    public int deleteNl2sqlMetricById(Long id);
}
