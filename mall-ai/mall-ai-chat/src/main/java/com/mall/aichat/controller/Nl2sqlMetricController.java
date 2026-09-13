package com.mall.aichat.controller;


import com.mall.aichat.domain.Nl2sqlMetric;
import com.mall.aichat.service.INl2sqlMetricService;
import com.mall.common.core.utils.poi.ExcelUtil;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.common.core.web.page.TableDataInfo;
import com.mall.common.log.annotation.Log;
import com.mall.common.log.enums.BusinessType;
import com.mall.common.security.annotation.RequiresPermissions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NL2SQL指标定义Controller
 * 
 * @author mall
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/metric")
public class Nl2sqlMetricController extends BaseController
{
    @Autowired
    private INl2sqlMetricService nl2sqlMetricService;

    /**
     * 查询NL2SQL指标定义列表
     */
    @RequiresPermissions("aichat:metric:list")
    @GetMapping("/list")
    public TableDataInfo list(Nl2sqlMetric nl2sqlMetric)
    {
        startPage();
        List<Nl2sqlMetric> list = nl2sqlMetricService.selectNl2sqlMetricList(nl2sqlMetric);
        return getDataTable(list);
    }

    /**
     * 导出NL2SQL指标定义列表
     */
    @RequiresPermissions("aichat:metric:export")
    @Log(title = "NL2SQL指标定义", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Nl2sqlMetric nl2sqlMetric)
    {
        List<Nl2sqlMetric> list = nl2sqlMetricService.selectNl2sqlMetricList(nl2sqlMetric);
        ExcelUtil<Nl2sqlMetric> util = new ExcelUtil<Nl2sqlMetric>(Nl2sqlMetric.class);
        util.exportExcel(response, list, "NL2SQL指标定义数据");
    }

    /**
     * 获取NL2SQL指标定义详细信息
     */
    @RequiresPermissions("aichat:metric:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(nl2sqlMetricService.selectNl2sqlMetricById(id));
    }

    /**
     * 新增NL2SQL指标定义
     */
    @RequiresPermissions("aichat:metric:add")
    @Log(title = "NL2SQL指标定义", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Nl2sqlMetric nl2sqlMetric)
    {
        return toAjax(nl2sqlMetricService.insertNl2sqlMetric(nl2sqlMetric));
    }

    /**
     * 修改NL2SQL指标定义
     */
    @RequiresPermissions("aichat:metric:edit")
    @Log(title = "NL2SQL指标定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Nl2sqlMetric nl2sqlMetric)
    {
        return toAjax(nl2sqlMetricService.updateNl2sqlMetric(nl2sqlMetric));
    }

    /**
     * 删除NL2SQL指标定义
     */
    @RequiresPermissions("aichat:metric:remove")
    @Log(title = "NL2SQL指标定义", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(nl2sqlMetricService.deleteNl2sqlMetricByIds(ids));
    }
}
