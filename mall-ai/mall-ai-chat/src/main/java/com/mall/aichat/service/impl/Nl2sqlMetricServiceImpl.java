package com.mall.aichat.service.impl;

import com.mall.aichat.domain.Nl2sqlMetric;
import com.mall.aichat.mapper.Nl2sqlMetricMapper;
import com.mall.aichat.service.INl2sqlMetricService;
import com.mall.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * NL2SQL指标定义Service业务层处理
 * 
 * @author mall
 * @date 2026-09-10
 */
@Service
public class Nl2sqlMetricServiceImpl implements INl2sqlMetricService 
{
    @Autowired
    private Nl2sqlMetricMapper nl2sqlMetricMapper;

    /**
     * 查询NL2SQL指标定义
     * 
     * @param id NL2SQL指标定义主键
     * @return NL2SQL指标定义
     */
    @Override
    public Nl2sqlMetric selectNl2sqlMetricById(Long id)
    {
        return nl2sqlMetricMapper.selectNl2sqlMetricById(id);
    }

    /**
     * 查询NL2SQL指标定义列表
     * 
     * @param nl2sqlMetric NL2SQL指标定义
     * @return NL2SQL指标定义
     */
    @Override
    public List<Nl2sqlMetric> selectNl2sqlMetricList(Nl2sqlMetric nl2sqlMetric)
    {
        return nl2sqlMetricMapper.selectNl2sqlMetricList(nl2sqlMetric);
    }

    /**
     * 新增NL2SQL指标定义
     * 
     * @param nl2sqlMetric NL2SQL指标定义
     * @return 结果
     */
    @Override
    public int insertNl2sqlMetric(Nl2sqlMetric nl2sqlMetric)
    {
        nl2sqlMetric.setCreateTime(DateUtils.getNowDate());
        return nl2sqlMetricMapper.insertNl2sqlMetric(nl2sqlMetric);
    }

    /**
     * 修改NL2SQL指标定义
     * 
     * @param nl2sqlMetric NL2SQL指标定义
     * @return 结果
     */
    @Override
    public int updateNl2sqlMetric(Nl2sqlMetric nl2sqlMetric)
    {
        nl2sqlMetric.setUpdateTime(DateUtils.getNowDate());
        return nl2sqlMetricMapper.updateNl2sqlMetric(nl2sqlMetric);
    }

    /**
     * 批量删除NL2SQL指标定义
     * 
     * @param ids 需要删除的NL2SQL指标定义主键
     * @return 结果
     */
    @Override
    public int deleteNl2sqlMetricByIds(Long[] ids)
    {
        return nl2sqlMetricMapper.deleteNl2sqlMetricByIds(ids);
    }

    /**
     * 删除NL2SQL指标定义信息
     * 
     * @param id NL2SQL指标定义主键
     * @return 结果
     */
    @Override
    public int deleteNl2sqlMetricById(Long id)
    {
        return nl2sqlMetricMapper.deleteNl2sqlMetricById(id);
    }
}
