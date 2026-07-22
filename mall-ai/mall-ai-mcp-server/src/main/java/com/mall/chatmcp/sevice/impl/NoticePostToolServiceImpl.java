package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysNoticeBo;
import com.mall.chatmcp.bo.SysPostBo;
import com.mall.chatmcp.sevice.BaseToolService;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteNoticeService;
import com.mall.system.api.RemotePostService;
import com.mall.system.api.domain.SysNotice;
import com.mall.system.api.domain.SysPost;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

@Service
public class NoticePostToolServiceImpl implements BaseToolService {

    @Autowired
    private RemoteNoticeService remoteNoticeService;

    @Autowired
    private RemotePostService remotePostService;

    @Autowired
    private Validator validator;

    @Tool(description = "通知公告的新增、修改、删除。参数包含 operationType(add/update/delete)和公告实体。 [JSON]")
    public AjaxResult noticeCrud(SysNoticeBo noticeBo) {
        String operationType = noticeBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }
        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handleNoticeAdd(noticeBo);
                case "update" -> handleNoticeUpdate(noticeBo);
                case "delete" -> handleNoticeDelete(noticeBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
            };
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，操作失败，请稍后再试。");
        }
    }

    private AjaxResult handleNoticeAdd(SysNoticeBo noticeBo) {
        BindingResult bindingResult = new BeanPropertyBindingResult(noticeBo, "sysNoticeBo");
        validator.validate(noticeBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        R<Boolean> result = remoteNoticeService.addNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeUpdate(SysNoticeBo noticeBo) {
        if (noticeBo.getNoticeId() == null) {
            return AjaxResult.error("修改操作必须传入公告ID");
        }
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        R<Boolean> result = remoteNoticeService.updateNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeDelete(SysNoticeBo noticeBo) {
        if (noticeBo.getNoticeId() == null) {
            return AjaxResult.error("删除操作必须传入公告ID");
        }
        R<Boolean> result = remoteNoticeService.deleteNotice(noticeBo.getNoticeId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "岗位数据的新增、修改、删除。参数包含 operationType(add/update/delete)和岗位实体。 [JSON]")
    public AjaxResult postCrud(SysPostBo postBo) {
        String operationType = postBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }
        try {
            return switch (operationType.toLowerCase()) {
                case "add" -> handlePostAdd(postBo);
                case "update" -> handlePostUpdate(postBo);
                case "delete" -> handlePostDelete(postBo);
                default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
            };
        } catch (Exception e) {
            return AjaxResult.error("系统内部异常，操作失败，请稍后再试。");
        }
    }

    private AjaxResult handlePostAdd(SysPostBo postBo) {
        BindingResult bindingResult = new BeanPropertyBindingResult(postBo, "sysPostBo");
        validator.validate(postBo, bindingResult);
        if (bindingResult.hasErrors()) {
            StringBuilder errorMsg = new StringBuilder("新增失败，原因：");
            bindingResult.getFieldErrors().forEach(error -> errorMsg.append(error.getDefaultMessage()).append("；"));
            return AjaxResult.error(errorMsg.toString());
        }
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        R<Boolean> result = remotePostService.addPost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostUpdate(SysPostBo postBo) {
        if (postBo.getPostId() == null) {
            return AjaxResult.error("修改操作必须传入岗位ID");
        }
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        R<Boolean> result = remotePostService.updatePost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostDelete(SysPostBo postBo) {
        if (postBo.getPostId() == null) {
            return AjaxResult.error("删除操作必须传入岗位ID");
        }
        R<Boolean> result = remotePostService.deletePost(postBo.getPostId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

}