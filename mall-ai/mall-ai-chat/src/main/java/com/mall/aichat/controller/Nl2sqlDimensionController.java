package com.mall.aichat.controller;


import com.mall.aichat.domain.Nl2sqlDimension;
import com.mall.aichat.service.INl2sqlDimensionService;
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
 * NL2SQL维度定义Controller
 * 
 * @author mall
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/dimension")
public class Nl2sqlDimensionController extends BaseController
{
    @Autowired
    private INl2sqlDimensionService nl2sqlDimensionService;

    /**
     * 查询NL2SQL维度定义列表
     */
    @RequiresPermissions("aichat:dimension:list")
    @GetMapping("/list")
    public TableDataInfo list(Nl2sqlDimension nl2sqlDimension)
    {
        startPage();
        List<Nl2sqlDimension> list = nl2sqlDimensionService.selectNl2sqlDimensionList(nl2sqlDimension);
        return getDataTable(list);
    }

    /**
     * 导出NL2SQL维度定义列表
     */
    @RequiresPermissions("aichat:dimension:export")
    @Log(title = "NL2SQL维度定义", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Nl2sqlDimension nl2sqlDimension)
    {
        List<Nl2sqlDimension> list = nl2sqlDimensionService.selectNl2sqlDimensionList(nl2sqlDimension);
        ExcelUtil<Nl2sqlDimension> util = new ExcelUtil<Nl2sqlDimension>(Nl2sqlDimension.class);
        util.exportExcel(response, list, "NL2SQL维度定义数据");
    }

    /**
     * 获取NL2SQL维度定义详细信息
     */
    @RequiresPermissions("aichat:dimension:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(nl2sqlDimensionService.selectNl2sqlDimensionById(id));
    }

    /**
     * 新增NL2SQL维度定义
     */
    @RequiresPermissions("aichat:dimension:add")
    @Log(title = "NL2SQL维度定义", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Nl2sqlDimension nl2sqlDimension)
    {
        return toAjax(nl2sqlDimensionService.insertNl2sqlDimension(nl2sqlDimension));
    }

    /**
     * 修改NL2SQL维度定义
     */
    @RequiresPermissions("aichat:dimension:edit")
    @Log(title = "NL2SQL维度定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Nl2sqlDimension nl2sqlDimension)
    {
        return toAjax(nl2sqlDimensionService.updateNl2sqlDimension(nl2sqlDimension));
    }

    /**
     * 删除NL2SQL维度定义
     */
    @RequiresPermissions("aichat:dimension:remove")
    @Log(title = "NL2SQL维度定义", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(nl2sqlDimensionService.deleteNl2sqlDimensionByIds(ids));
    }
}
