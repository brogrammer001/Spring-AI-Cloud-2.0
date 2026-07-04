package com.mall.aichat.controller;

import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.ISysChatHistoryService;
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
 * @date 2026-07-04
 */
@RestController
@RequestMapping("/history")
public class SysChatHistoryController extends BaseController
{
    @Autowired
    private ISysChatHistoryService sysChatHistoryService;

    /**
     * 查询【请填写功能名称】列表
     */
    @RequiresPermissions("aichat:history:list")
    @GetMapping("/list")
    public TableDataInfo list(SysChatHistory sysChatHistory)
    {
        startPage();
        List<SysChatHistory> list = sysChatHistoryService.selectSysChatHistoryList(sysChatHistory);
        return getDataTable(list);
    }

    /**
     * 导出【请填写功能名称】列表
     */
    @RequiresPermissions("aichat:history:export")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysChatHistory sysChatHistory)
    {
        List<SysChatHistory> list = sysChatHistoryService.selectSysChatHistoryList(sysChatHistory);
        ExcelUtil<SysChatHistory> util = new ExcelUtil<SysChatHistory>(SysChatHistory.class);
        util.exportExcel(response, list, "【请填写功能名称】数据");
    }

    /**
     * 获取【请填写功能名称】详细信息
     */
    @RequiresPermissions("aichat:history:query")
    @GetMapping(value = "/{conversationId}")
    public AjaxResult getInfo(@PathVariable("conversationId") String conversationId)
    {
        return success(sysChatHistoryService.selectSysChatHistoryByConversationId(conversationId));
    }

    /**
     * 新增【请填写功能名称】
     */
    @RequiresPermissions("aichat:history:add")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysChatHistory sysChatHistory)
    {
        return toAjax(sysChatHistoryService.insertSysChatHistory(sysChatHistory));
    }

    /**
     * 修改【请填写功能名称】
     */
    @RequiresPermissions("aichat:history:edit")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysChatHistory sysChatHistory)
    {
        return toAjax(sysChatHistoryService.updateSysChatHistory(sysChatHistory));
    }

    /**
     * 删除【请填写功能名称】
     */
    @RequiresPermissions("aichat:history:remove")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.DELETE)
	@DeleteMapping("/{conversationIds}")
    public AjaxResult remove(@PathVariable String[] conversationIds)
    {
        return toAjax(sysChatHistoryService.deleteSysChatHistoryByConversationIds(conversationIds));
    }

    /**
     * 查询会话历史
     */
    @RequiresPermissions("aichat:history:getConversationListByUserId")
    @GetMapping("/getChatMemoryListByConversationId/{conversationId}")
    public AjaxResult getConversationListByUserId(@PathVariable("conversationId") String conversationId) {
        return success(sysChatHistoryService.selectSysChatHistoryByConversationId(conversationId));
    }
}
