package com.mall.aichat.controller;

import com.mall.aichat.domain.Nl2sqlBusinessRule;
import com.mall.aichat.service.INl2sqlBusinessRuleService;
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
 * NL2SQL业务规则Controller
 * 
 * @author mall
 * @date 2026-09-10
 */
@RestController
@RequestMapping("/rule")
public class Nl2sqlBusinessRuleController extends BaseController
{
    @Autowired
    private INl2sqlBusinessRuleService nl2sqlBusinessRuleService;

    /**
     * 查询NL2SQL业务规则列表
     */
    @RequiresPermissions("aichat:rule:list")
    @GetMapping("/list")
    public TableDataInfo list(Nl2sqlBusinessRule nl2sqlBusinessRule)
    {
        startPage();
        List<Nl2sqlBusinessRule> list = nl2sqlBusinessRuleService.selectNl2sqlBusinessRuleList(nl2sqlBusinessRule);
        return getDataTable(list);
    }

    /**
     * 导出NL2SQL业务规则列表
     */
    @RequiresPermissions("aichat:rule:export")
    @Log(title = "NL2SQL业务规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Nl2sqlBusinessRule nl2sqlBusinessRule)
    {
        List<Nl2sqlBusinessRule> list = nl2sqlBusinessRuleService.selectNl2sqlBusinessRuleList(nl2sqlBusinessRule);
        ExcelUtil<Nl2sqlBusinessRule> util = new ExcelUtil<Nl2sqlBusinessRule>(Nl2sqlBusinessRule.class);
        util.exportExcel(response, list, "NL2SQL业务规则数据");
    }

    /**
     * 获取NL2SQL业务规则详细信息
     */
    @RequiresPermissions("aichat:rule:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(nl2sqlBusinessRuleService.selectNl2sqlBusinessRuleById(id));
    }

    /**
     * 新增NL2SQL业务规则
     */
    @RequiresPermissions("aichat:rule:add")
    @Log(title = "NL2SQL业务规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Nl2sqlBusinessRule nl2sqlBusinessRule)
    {
        return toAjax(nl2sqlBusinessRuleService.insertNl2sqlBusinessRule(nl2sqlBusinessRule));
    }

    /**
     * 修改NL2SQL业务规则
     */
    @RequiresPermissions("aichat:rule:edit")
    @Log(title = "NL2SQL业务规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Nl2sqlBusinessRule nl2sqlBusinessRule)
    {
        return toAjax(nl2sqlBusinessRuleService.updateNl2sqlBusinessRule(nl2sqlBusinessRule));
    }

    /**
     * 删除NL2SQL业务规则
     */
    @RequiresPermissions("aichat:rule:remove")
    @Log(title = "NL2SQL业务规则", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(nl2sqlBusinessRuleService.deleteNl2sqlBusinessRuleByIds(ids));
    }
}
