package com.mall.chatrag.controller;

import com.mall.chatrag.domain.KbDocumentChunk;
import com.mall.chatrag.service.IKbDocumentChunkService;
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
 * 文档切片Controller
 * 
 * @author mall
 * @date 2026-07-05
 */
@RestController
@RequestMapping("/chunk")
public class KbDocumentChunkController extends BaseController
{
    @Autowired
    private IKbDocumentChunkService kbDocumentChunkService;

    /**
     * 查询文档切片列表
     */
    @RequiresPermissions("chatrag:chunk:list")
    @GetMapping("/list")
    public TableDataInfo list(KbDocumentChunk kbDocumentChunk)
    {
        startPage();
        List<KbDocumentChunk> list = kbDocumentChunkService.selectKbDocumentChunkList(kbDocumentChunk);
        return getDataTable(list);
    }

    /**
     * 导出文档切片列表
     */
    @RequiresPermissions("chatrag:chunk:export")
    @Log(title = "文档切片", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, KbDocumentChunk kbDocumentChunk)
    {
        List<KbDocumentChunk> list = kbDocumentChunkService.selectKbDocumentChunkList(kbDocumentChunk);
        ExcelUtil<KbDocumentChunk> util = new ExcelUtil<KbDocumentChunk>(KbDocumentChunk.class);
        util.exportExcel(response, list, "文档切片数据");
    }

    /**
     * 获取文档切片详细信息
     */
    @RequiresPermissions("chatrag:chunk:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(kbDocumentChunkService.selectKbDocumentChunkById(id));
    }

    /**
     * 新增文档切片
     */
    @RequiresPermissions("chatrag:chunk:add")
    @Log(title = "文档切片", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KbDocumentChunk kbDocumentChunk)
    {
        return toAjax(kbDocumentChunkService.insertKbDocumentChunk(kbDocumentChunk));
    }

    /**
     * 修改文档切片
     */
    @RequiresPermissions("chatrag:chunk:edit")
    @Log(title = "文档切片", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KbDocumentChunk kbDocumentChunk)
    {
        return toAjax(kbDocumentChunkService.updateKbDocumentChunk(kbDocumentChunk));
    }

    /**
     * 删除文档切片
     */
    @RequiresPermissions("chatrag:chunk:remove")
    @Log(title = "文档切片", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(kbDocumentChunkService.deleteKbDocumentChunkByIds(ids));
    }
}
