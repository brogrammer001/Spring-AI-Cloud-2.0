package com.mall.chatrag.controller;

import com.mall.chatrag.domain.KbDocument;
import com.mall.chatrag.service.IKbDocumentService;
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
 * 知识库文档Controller
 * 
 * @author mall
 * @date 2026-07-05
 */
@RestController
@RequestMapping("/document")
public class KbDocumentController extends BaseController
{
    @Autowired
    private IKbDocumentService kbDocumentService;

    /**
     * 查询知识库文档列表
     */
    @RequiresPermissions("chatrag:document:list")
    @GetMapping("/list")
    public TableDataInfo list(KbDocument kbDocument)
    {
        startPage();
        List<KbDocument> list = kbDocumentService.selectKbDocumentList(kbDocument);
        return getDataTable(list);
    }

    /**
     * 导出知识库文档列表
     */
    @RequiresPermissions("chatrag:document:export")
    @Log(title = "知识库文档", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, KbDocument kbDocument)
    {
        List<KbDocument> list = kbDocumentService.selectKbDocumentList(kbDocument);
        ExcelUtil<KbDocument> util = new ExcelUtil<KbDocument>(KbDocument.class);
        util.exportExcel(response, list, "知识库文档数据");
    }

    /**
     * 获取知识库文档详细信息
     */
    @RequiresPermissions("chatrag:document:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(kbDocumentService.selectKbDocumentById(id));
    }

    /**
     * 新增知识库文档
     */
    @RequiresPermissions("chatrag:document:add")
    @Log(title = "知识库文档", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KbDocument kbDocument)
    {
        return toAjax(kbDocumentService.insertKbDocument(kbDocument));
    }

    /**
     * 修改知识库文档
     */
    @RequiresPermissions("chatrag:document:edit")
    @Log(title = "知识库文档", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KbDocument kbDocument)
    {
        return toAjax(kbDocumentService.updateKbDocument(kbDocument));
    }

    /**
     * 删除知识库文档
     */
    @RequiresPermissions("chatrag:document:remove")
    @Log(title = "知识库文档", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(kbDocumentService.deleteKbDocumentByIds(ids));
    }
}
