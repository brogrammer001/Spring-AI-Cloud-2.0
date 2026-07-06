package com.mall.aichat.controller;

import com.mall.aichat.domain.KbKnowledgeBase;
import com.mall.aichat.service.IKbKnowledgeBaseService;
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
 * 知识库Controller
 * 
 * @author mall
 * @date 2026-07-05
 */
@RestController
@RequestMapping("/base")
public class KbKnowledgeBaseController extends BaseController
{
    @Autowired
    private IKbKnowledgeBaseService kbKnowledgeBaseService;

    /**
     * 查询知识库列表
     */
    @RequiresPermissions("aichat:base:list")
    @GetMapping("/list")
    public TableDataInfo list(KbKnowledgeBase kbKnowledgeBase)
    {
        startPage();
        List<KbKnowledgeBase> list = kbKnowledgeBaseService.selectKbKnowledgeBaseList(kbKnowledgeBase);
        return getDataTable(list);
    }

    /**
     * 导出知识库列表
     */
    @RequiresPermissions("aichat:base:export")
    @Log(title = "知识库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, KbKnowledgeBase kbKnowledgeBase)
    {
        List<KbKnowledgeBase> list = kbKnowledgeBaseService.selectKbKnowledgeBaseList(kbKnowledgeBase);
        ExcelUtil<KbKnowledgeBase> util = new ExcelUtil<KbKnowledgeBase>(KbKnowledgeBase.class);
        util.exportExcel(response, list, "知识库数据");
    }

    /**
     * 获取知识库详细信息
     */
    @RequiresPermissions("aichat:base:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(kbKnowledgeBaseService.selectKbKnowledgeBaseById(id));
    }

    /**
     * 新增知识库
     */
    @RequiresPermissions("aichat:base:add")
    @Log(title = "知识库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KbKnowledgeBase kbKnowledgeBase)
    {
        return toAjax(kbKnowledgeBaseService.insertKbKnowledgeBase(kbKnowledgeBase));
    }

    /**
     * 修改知识库
     */
    @RequiresPermissions("aichat:base:edit")
    @Log(title = "知识库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KbKnowledgeBase kbKnowledgeBase)
    {
        return toAjax(kbKnowledgeBaseService.updateKbKnowledgeBase(kbKnowledgeBase));
    }

    /**
     * 删除知识库
     */
    @RequiresPermissions("aichat:base:remove")
    @Log(title = "知识库", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(kbKnowledgeBaseService.deleteKbKnowledgeBaseByIds(ids));
    }
}
