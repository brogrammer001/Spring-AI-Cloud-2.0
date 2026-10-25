package com.mall.aichat.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mall.common.log.annotation.Log;
import com.mall.common.log.enums.BusinessType;
import com.mall.common.security.annotation.RequiresPermissions;
import com.mall.aichat.domain.AiMemoryFact;
import com.mall.aichat.service.IAiMemoryFactService;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.common.core.utils.poi.ExcelUtil;
import com.mall.common.core.web.page.TableDataInfo;

/**
 * AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)Controller
 * 
 * @author mall
 * @date 2026-10-24
 */
@RestController
@RequestMapping("/fact")
public class AiMemoryFactController extends BaseController
{
    @Autowired
    private IAiMemoryFactService aiMemoryFactService;

    /**
     * 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)列表
     */
    @RequiresPermissions("aichat:fact:list")
    @GetMapping("/list")
    public TableDataInfo list(AiMemoryFact aiMemoryFact)
    {
        startPage();
        List<AiMemoryFact> list = aiMemoryFactService.selectAiMemoryFactList(aiMemoryFact);
        return getDataTable(list);
    }

    /**
     * 导出AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)列表
     */
    @RequiresPermissions("aichat:fact:export")
    @Log(title = "AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AiMemoryFact aiMemoryFact)
    {
        List<AiMemoryFact> list = aiMemoryFactService.selectAiMemoryFactList(aiMemoryFact);
        ExcelUtil<AiMemoryFact> util = new ExcelUtil<AiMemoryFact>(AiMemoryFact.class);
        util.exportExcel(response, list, "AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)数据");
    }

    /**
     * 获取AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)详细信息
     */
    @RequiresPermissions("aichat:fact:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(aiMemoryFactService.selectAiMemoryFactById(id));
    }

    /**
     * 新增AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     */
    @RequiresPermissions("aichat:fact:add")
    @Log(title = "AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiMemoryFact aiMemoryFact)
    {
        return toAjax(aiMemoryFactService.insertAiMemoryFact(aiMemoryFact));
    }

    /**
     * 修改AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     */
    @RequiresPermissions("aichat:fact:edit")
    @Log(title = "AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiMemoryFact aiMemoryFact)
    {
        return toAjax(aiMemoryFactService.updateAiMemoryFact(aiMemoryFact));
    }

    /**
     * 同步选中的长期记忆到向量库：先删除旧向量，再按 MySQL 数据重建索引
     */
    @RequiresPermissions("aichat:fact:edit")
    @Log(title = "AI长期记忆事实", businessType = BusinessType.UPDATE)
    @PostMapping("/syncVector")
    public AjaxResult syncVector(@RequestBody Long[] ids)
    {
        int synced = aiMemoryFactService.syncVectorStore(ids);
        return AjaxResult.success("向量同步完成，共 " + synced + " 条", synced);
    }

    /**
     * 删除AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     */
    @RequiresPermissions("aichat:fact:remove")
    @Log(title = "AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiMemoryFactService.deleteAiMemoryFactByIds(ids));
    }
}
