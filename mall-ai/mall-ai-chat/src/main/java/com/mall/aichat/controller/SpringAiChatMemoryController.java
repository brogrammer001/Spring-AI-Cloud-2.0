package com.mall.aichat.controller;

import com.mall.aichat.domain.SpringAiChatMemory;
import com.mall.aichat.service.ISpringAiChatMemoryService;
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
 * 【请填写功能名称】Controller
 * 
 * @author mall
 * @date 2026-06-27
 */
@RestController
@RequestMapping("/chatmemory")
public class SpringAiChatMemoryController extends BaseController
{
    @Autowired
    private ISpringAiChatMemoryService springAiChatMemoryService;

    /**
     * 查询【请填写功能名称】列表
     */
    @RequiresPermissions("aichat:chatmemory:list")
    @GetMapping("/list")
    public TableDataInfo list(SpringAiChatMemory springAiChatMemory)
    {
        startPage();
        List<SpringAiChatMemory> list = springAiChatMemoryService.selectSpringAiChatMemoryList(springAiChatMemory);
        return getDataTable(list);
    }

    /**
     * 导出【请填写功能名称】列表
     */
    @RequiresPermissions("aichat:chatmemory:export")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SpringAiChatMemory springAiChatMemory)
    {
        List<SpringAiChatMemory> list = springAiChatMemoryService.selectSpringAiChatMemoryList(springAiChatMemory);
        ExcelUtil<SpringAiChatMemory> util = new ExcelUtil<SpringAiChatMemory>(SpringAiChatMemory.class);
        util.exportExcel(response, list, "【请填写功能名称】数据");
    }

    /**
     * 获取【请填写功能名称】详细信息
     */
    @RequiresPermissions("aichat:chatmemory:query")
    @GetMapping(value = "/{conversationId}")
    public AjaxResult getInfo(@PathVariable("conversationId") String conversationId)
    {
        return success(springAiChatMemoryService.selectSpringAiChatMemoryByConversationId(conversationId));
    }

    /**
     * 新增【请填写功能名称】
     */
    @RequiresPermissions("aichat:chatmemory:add")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SpringAiChatMemory springAiChatMemory)
    {
        return toAjax(springAiChatMemoryService.insertSpringAiChatMemory(springAiChatMemory));
    }

    /**
     * 修改【请填写功能名称】
     */
    @RequiresPermissions("aichat:chatmemory:edit")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SpringAiChatMemory springAiChatMemory)
    {
        return toAjax(springAiChatMemoryService.updateSpringAiChatMemory(springAiChatMemory));
    }

    /**
     * 删除【请填写功能名称】
     */
    @RequiresPermissions("aichat:chatmemory:remove")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.DELETE)
	@DeleteMapping("/{conversationIds}")
    public AjaxResult remove(@PathVariable String[] conversationIds)
    {
        return toAjax(springAiChatMemoryService.deleteSpringAiChatMemoryByConversationIds(conversationIds));
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @RequiresPermissions("aichat:conversation:getConversationListByUserId")
    @GetMapping("/getChatMemoryListByConversationId/{conversationId}")
    public AjaxResult getConversationListByUserId(@PathVariable("conversationId") String conversationId) {
        return success(springAiChatMemoryService.selectSpringAiChatMemoryByConversationId(conversationId));
    }
}
