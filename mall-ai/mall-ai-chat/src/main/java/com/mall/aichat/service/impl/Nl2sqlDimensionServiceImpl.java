package com.mall.aichat.service.impl;


import com.mall.aichat.domain.Nl2sqlDimension;
import com.mall.aichat.mapper.Nl2sqlDimensionMapper;
import com.mall.aichat.service.INl2sqlDimensionService;
import com.mall.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * NL2SQL维度定义Service业务层处理
 * 
 * @author mall
 * @date 2026-09-10
 */
@Service
public class Nl2sqlDimensionServiceImpl implements INl2sqlDimensionService 
{
    @Autowired
    private Nl2sqlDimensionMapper nl2sqlDimensionMapper;

    /**
     * 查询NL2SQL维度定义
     * 
     * @param id NL2SQL维度定义主键
     * @return NL2SQL维度定义
     */
    @Override
    public Nl2sqlDimension selectNl2sqlDimensionById(Long id)
    {
        return nl2sqlDimensionMapper.selectNl2sqlDimensionById(id);
    }

    /**
     * 查询NL2SQL维度定义列表
     * 
     * @param nl2sqlDimension NL2SQL维度定义
     * @return NL2SQL维度定义
     */
    @Override
    public List<Nl2sqlDimension> selectNl2sqlDimensionList(Nl2sqlDimension nl2sqlDimension)
    {
        return nl2sqlDimensionMapper.selectNl2sqlDimensionList(nl2sqlDimension);
    }

    /**
     * 新增NL2SQL维度定义
     * 
     * @param nl2sqlDimension NL2SQL维度定义
     * @return 结果
     */
    @Override
    public int insertNl2sqlDimension(Nl2sqlDimension nl2sqlDimension)
    {
        nl2sqlDimension.setCreateTime(DateUtils.getNowDate());
        return nl2sqlDimensionMapper.insertNl2sqlDimension(nl2sqlDimension);
    }

    /**
     * 修改NL2SQL维度定义
     * 
     * @param nl2sqlDimension NL2SQL维度定义
     * @return 结果
     */
    @Override
    public int updateNl2sqlDimension(Nl2sqlDimension nl2sqlDimension)
    {
        nl2sqlDimension.setUpdateTime(DateUtils.getNowDate());
        return nl2sqlDimensionMapper.updateNl2sqlDimension(nl2sqlDimension);
    }

    /**
     * 批量删除NL2SQL维度定义
     * 
     * @param ids 需要删除的NL2SQL维度定义主键
     * @return 结果
     */
    @Override
    public int deleteNl2sqlDimensionByIds(Long[] ids)
    {
        return nl2sqlDimensionMapper.deleteNl2sqlDimensionByIds(ids);
    }

    /**
     * 删除NL2SQL维度定义信息
     * 
     * @param id NL2SQL维度定义主键
     * @return 结果
     */
    @Override
    public int deleteNl2sqlDimensionById(Long id)
    {
        return nl2sqlDimensionMapper.deleteNl2sqlDimensionById(id);
    }
}
