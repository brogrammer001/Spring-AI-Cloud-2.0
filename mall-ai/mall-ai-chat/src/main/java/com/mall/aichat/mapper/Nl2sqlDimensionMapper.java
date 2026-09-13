package com.mall.aichat.mapper;

import com.mall.aichat.domain.Nl2sqlDimension;

import java.util.List;

/**
 * NL2SQL维度定义Mapper接口
 * 
 * @author mall
 * @date 2026-09-10
 */
public interface Nl2sqlDimensionMapper 
{
    /**
     * 查询NL2SQL维度定义
     * 
     * @param id NL2SQL维度定义主键
     * @return NL2SQL维度定义
     */
    public Nl2sqlDimension selectNl2sqlDimensionById(Long id);

    /**
     * 查询NL2SQL维度定义列表
     * 
     * @param nl2sqlDimension NL2SQL维度定义
     * @return NL2SQL维度定义集合
     */
    public List<Nl2sqlDimension> selectNl2sqlDimensionList(Nl2sqlDimension nl2sqlDimension);

    /**
     * 新增NL2SQL维度定义
     * 
     * @param nl2sqlDimension NL2SQL维度定义
     * @return 结果
     */
    public int insertNl2sqlDimension(Nl2sqlDimension nl2sqlDimension);

    /**
     * 修改NL2SQL维度定义
     * 
     * @param nl2sqlDimension NL2SQL维度定义
     * @return 结果
     */
    public int updateNl2sqlDimension(Nl2sqlDimension nl2sqlDimension);

    /**
     * 删除NL2SQL维度定义
     * 
     * @param id NL2SQL维度定义主键
     * @return 结果
     */
    public int deleteNl2sqlDimensionById(Long id);

    /**
     * 批量删除NL2SQL维度定义
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteNl2sqlDimensionByIds(Long[] ids);
}
