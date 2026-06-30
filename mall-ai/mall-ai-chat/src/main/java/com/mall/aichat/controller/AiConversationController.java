package com.mall.aichat.controller;

import com.mall.aichat.domain.AiConversation;
import com.mall.aichat.service.IAiConversationService;
import com.mall.common.core.utils.poi.ExcelUtil;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.common.core.web.page.TableDataInfo;
import com.mall.common.log.annotation.Log;
import com.mall.common.log.enums.BusinessType;
import com.mall.common.security.annotation.RequiresPermissions;
import com.mall.common.security.utils.SecurityUtils;
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
@RequestMapping("/conversation")
public class AiConversationController extends BaseController {
    @Autowired
    private IAiConversationService aiConversationService;

    /**
     * 查询【请填写功能名称】列表
     */
    @RequiresPermissions("aichat:conversation:list")
    @GetMapping("/list")
    public TableDataInfo list(AiConversation aiConversation) {
        startPage();
        List<AiConversation> list = aiConversationService.selectAiConversationList(aiConversation);
        return getDataTable(list);
    }

    /**
     * 导出【请填写功能名称】列表
     */
    @RequiresPermissions("aichat:conversation:export")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiConversation aiConversation) {
        List<AiConversation> list = aiConversationService.selectAiConversationList(aiConversation);
        ExcelUtil<AiConversation> util = new ExcelUtil<AiConversation>(AiConversation.class);
        util.exportExcel(response, list, "【请填写功能名称】数据");
    }

    /**
     * 获取【请填写功能名称】详细信息
     */
    @RequiresPermissions("aichat:conversation:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(aiConversationService.selectAiConversationById(id));
    }

    /**
     * 新增【请填写功能名称】
     */
    @RequiresPermissions("aichat:conversation:add")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiConversation aiConversation) {
        return toAjax(aiConversationService.insertAiConversation(aiConversation));
    }

    /**
     * 修改【请填写功能名称】
     */
    @RequiresPermissions("aichat:conversation:edit")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiConversation aiConversation) {
        return toAjax(aiConversationService.updateAiConversation(aiConversation));
    }

    /**
     * 删除【请填写功能名称】
     */
    @RequiresPermissions("aichat:conversation:remove")
    @Log(title = "【请填写功能名称】", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(aiConversationService.deleteAiConversationByIds(ids));
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @RequiresPermissions("aichat:conversation:getConversationListByUserId")
    @GetMapping("/getConversationListByUserId")
    public AjaxResult getConversationListByUserId() {
        AiConversation param = new AiConversation();
        param.setUserId(SecurityUtils.getUserId());
        return success(aiConversationService.selectAiConversationList(param));
    }

    /**
     * 新增【请填写功能名称】
     */
    @RequiresPermissions("aichat:conversation:create")
    @Log( businessType = BusinessType.INSERT)
    @GetMapping("/create")
    public AjaxResult create(@RequestParam String question) {
        return success(aiConversationService.createAiConversation(question));
    }

    /**
     * 删除【请填写功能名称】
     */
    @RequiresPermissions("aichat:conversation:deleteByConversationId")
    @Log(title = "删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/deleteByConversationId/{conversationIds}")
    public AjaxResult deleteByConversationId(@PathVariable String[] conversationIds) {
        return toAjax(aiConversationService.deleteByConversationId(conversationIds));
    }
}
