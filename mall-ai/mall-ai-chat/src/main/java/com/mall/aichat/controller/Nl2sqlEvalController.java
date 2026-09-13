package com.mall.aichat.controller;


import com.mall.aichat.domain.Nl2sqlEval;
import com.mall.aichat.service.INl2sqlEvalService;
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
 * NL2SQL黄金评测集Controller
 * 
 * @author mall
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/eval")
public class Nl2sqlEvalController extends BaseController
{
    @Autowired
    private INl2sqlEvalService nl2sqlEvalService;

    /**
     * 查询NL2SQL黄金评测集列表
     */
    @RequiresPermissions("aichat:eval:list")
    @GetMapping("/list")
    public TableDataInfo list(Nl2sqlEval nl2sqlEval)
    {
        startPage();
        List<Nl2sqlEval> list = nl2sqlEvalService.selectNl2sqlEvalList(nl2sqlEval);
        return getDataTable(list);
    }

    /**
     * 导出NL2SQL黄金评测集列表
     */
    @RequiresPermissions("aichat:eval:export")
    @Log(title = "NL2SQL黄金评测集", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Nl2sqlEval nl2sqlEval)
    {
        List<Nl2sqlEval> list = nl2sqlEvalService.selectNl2sqlEvalList(nl2sqlEval);
        ExcelUtil<Nl2sqlEval> util = new ExcelUtil<Nl2sqlEval>(Nl2sqlEval.class);
        util.exportExcel(response, list, "NL2SQL黄金评测集数据");
    }

    /**
     * 获取NL2SQL黄金评测集详细信息
     */
    @RequiresPermissions("aichat:eval:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(nl2sqlEvalService.selectNl2sqlEvalById(id));
    }

    /**
     * 新增NL2SQL黄金评测集
     */
    @RequiresPermissions("aichat:eval:add")
    @Log(title = "NL2SQL黄金评测集", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Nl2sqlEval nl2sqlEval)
    {
        return toAjax(nl2sqlEvalService.insertNl2sqlEval(nl2sqlEval));
    }

    /**
     * 修改NL2SQL黄金评测集
     */
    @RequiresPermissions("aichat:eval:edit")
    @Log(title = "NL2SQL黄金评测集", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Nl2sqlEval nl2sqlEval)
    {
        return toAjax(nl2sqlEvalService.updateNl2sqlEval(nl2sqlEval));
    }

    /**
     * 删除NL2SQL黄金评测集
     */
    @RequiresPermissions("aichat:eval:remove")
    @Log(title = "NL2SQL黄金评测集", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(nl2sqlEvalService.deleteNl2sqlEvalByIds(ids));
    }
}
