package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysNoticeBo;
import com.mall.chatmcp.bo.SysPostBo;
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
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class NoticePostToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteNoticeService remoteNoticeService;

    @Autowired
    private RemotePostService remotePostService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "通知公告的新增、修改、删除。参数包含 operationType(add/update/delete)和公告实体。")
    public AjaxResult noticeCrud(SysNoticeBo noticeBo) {
        String operationType = noticeBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }

        return executeWithErrorHandling(() -> switch (operationType.toLowerCase()) {
            case "add" -> handleNoticeAdd(noticeBo);
            case "update" -> handleNoticeUpdate(noticeBo);
            case "delete" -> handleNoticeDelete(noticeBo);
            default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
        }, "通知公告操作");
    }

    private AjaxResult handleNoticeAdd(SysNoticeBo noticeBo) {
        AjaxResult validateResult = validate(noticeBo, "sysNoticeBo");
        if (validateResult != null) {
            return validateResult;
        }
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        R<Boolean> result = remoteNoticeService.addNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeUpdate(SysNoticeBo noticeBo) {
        if (noticeBo.getNoticeTitle() == null || noticeBo.getNoticeTitle().isEmpty()) {
            return AjaxResult.error("修改操作必须传入公告标题");
        }
        List<SysNotice> notices = getNoticesByConditions(noticeBo);
        if (notices.isEmpty()) {
            return AjaxResult.error("公告不存在");
        }
        if (notices.size() > 1) {
            StringBuilder sb = new StringBuilder("查询到多条公告，请补充更多信息（如公告类型等）后重试：");
            for (SysNotice n : notices) {
                sb.append(String.format("[标题:%s, 类型:%s, 状态:%s] ", n.getNoticeTitle(), n.getNoticeType(),
                        "0".equals(n.getStatus()) ? "正常" : "关闭"));
            }
            return AjaxResult.error(sb.toString());
        }
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        sysNotice.setNoticeId(notices.getFirst().getNoticeId());
        R<Boolean> result = remoteNoticeService.updateNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeDelete(SysNoticeBo noticeBo) {
        if (noticeBo.getNoticeTitle() == null || noticeBo.getNoticeTitle().isEmpty()) {
            return AjaxResult.error("删除操作必须传入公告标题");
        }
        List<SysNotice> notices = getNoticesByConditions(noticeBo);
        if (notices.isEmpty()) {
            return AjaxResult.error("公告不存在");
        }
        if (notices.size() > 1) {
            StringBuilder sb = new StringBuilder("查询到多条公告，请补充更多信息（如公告类型等）后重试：");
            for (SysNotice n : notices) {
                sb.append(String.format("[标题:%s, 类型:%s, 状态:%s] ", n.getNoticeTitle(), n.getNoticeType(),
                        "0".equals(n.getStatus()) ? "正常" : "关闭"));
            }
            return AjaxResult.error(sb.toString());
        }
        R<Boolean> result = remoteNoticeService.deleteNotice(notices.getFirst().getNoticeId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "岗位数据的新增、修改、删除。参数包含 operationType(add/update/delete)和岗位实体。")
    public AjaxResult postCrud(SysPostBo postBo) {
        String operationType = postBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete");
        }

        return executeWithErrorHandling(() -> switch (operationType.toLowerCase()) {
            case "add" -> handlePostAdd(postBo);
            case "update" -> handlePostUpdate(postBo);
            case "delete" -> handlePostDelete(postBo);
            default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete");
        }, "岗位操作");
    }

    private AjaxResult handlePostAdd(SysPostBo postBo) {
        AjaxResult validateResult = validate(postBo, "sysPostBo");
        if (validateResult != null) {
            return validateResult;
        }
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        R<Boolean> result = remotePostService.addPost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostUpdate(SysPostBo postBo) {
        if (postBo.getPostName() == null || postBo.getPostName().isEmpty()) {
            return AjaxResult.error("修改操作必须传入岗位名称");
        }
        List<SysPost> posts = getPostsByConditions(postBo);
        if (posts.isEmpty()) {
            return AjaxResult.error("岗位不存在");
        }
        if (posts.size() > 1) {
            StringBuilder sb = new StringBuilder("查询到多个岗位，请补充更多信息（如岗位编码等）后重试：");
            for (SysPost p : posts) {
                sb.append(String.format("[岗位名称:%s, 编码:%s, 状态:%s] ", p.getPostName(), p.getPostCode(),
                        "0".equals(p.getStatus()) ? "正常" : "停用"));
            }
            return AjaxResult.error(sb.toString());
        }
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        sysPost.setPostId(posts.getFirst().getPostId());
        R<Boolean> result = remotePostService.updatePost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostDelete(SysPostBo postBo) {
        if (postBo.getPostName() == null || postBo.getPostName().isEmpty()) {
            return AjaxResult.error("删除操作必须传入岗位名称");
        }
        List<SysPost> posts = getPostsByConditions(postBo);
        if (posts.isEmpty()) {
            return AjaxResult.error("岗位不存在");
        }
        if (posts.size() > 1) {
            StringBuilder sb = new StringBuilder("查询到多个岗位，请补充更多信息（如岗位编码等）后重试：");
            for (SysPost p : posts) {
                sb.append(String.format("[岗位名称:%s, 编码:%s, 状态:%s] ", p.getPostName(), p.getPostCode(),
                        "0".equals(p.getStatus()) ? "正常" : "停用"));
            }
            return AjaxResult.error(sb.toString());
        }
        R<Boolean> result = remotePostService.deletePost(posts.getFirst().getPostId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private List<SysNotice> getNoticesByTitle(String noticeTitle) {
        SysNotice query = new SysNotice();
        query.setNoticeTitle(noticeTitle);
        R<List<SysNotice>> result = remoteNoticeService.getNoticeList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<SysPost> getPostsByName(String postName) {
        SysPost query = new SysPost();
        query.setPostName(postName);
        R<List<SysPost>> result = remotePostService.getPostList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<SysNotice> getNoticesByConditions(SysNoticeBo noticeBo) {
        SysNotice query = new SysNotice();
        if (noticeBo.getNoticeTitle() != null && !noticeBo.getNoticeTitle().isEmpty()) {
            query.setNoticeTitle(noticeBo.getNoticeTitle());
        }
        if (noticeBo.getNoticeType() != null && !noticeBo.getNoticeType().isEmpty()) {
            query.setNoticeType(noticeBo.getNoticeType());
        }
        R<List<SysNotice>> result = remoteNoticeService.getNoticeList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private List<SysPost> getPostsByConditions(SysPostBo postBo) {
        SysPost query = new SysPost();
        if (postBo.getPostName() != null && !postBo.getPostName().isEmpty()) {
            query.setPostName(postBo.getPostName());
        }
        if (postBo.getPostCode() != null && !postBo.getPostCode().isEmpty()) {
            query.setPostCode(postBo.getPostCode());
        }
        R<List<SysPost>> result = remotePostService.getPostList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }
}
