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

    @Tool(description = "通知公告的新增、修改、删除。参数包含 operationType(add/update/delete)和公告实体。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
    public AjaxResult noticeCrud(SysNoticeBo noticeBo) {
        AjaxResult gate = crudGate(noticeBo, "noticeCrud", "公告", OPS_CRUD, "noticeTitle");
        if (gate != null) {
            return gate;
        }
        return executeWithErrorHandling(() -> switch (noticeBo.getOperationType().toLowerCase()) {
            case "add" -> handleNoticeAdd(noticeBo);
            case "update" -> handleNoticeUpdate(noticeBo);
            case "delete" -> handleNoticeDelete(noticeBo);
            default -> AjaxResult.error("不支持的操作类型：" + noticeBo.getOperationType() + "，请使用：add、update、delete");
        }, "通知公告操作");
    }

    private AjaxResult handleNoticeAdd(SysNoticeBo noticeBo) {
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        R<Boolean> result = remoteNoticeService.addNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeUpdate(SysNoticeBo noticeBo) {
        if (noticeBo.getNoticeTitle() == null || noticeBo.getNoticeTitle().isEmpty()) {
            return formMissing("修改操作必须传入公告标题，请补充", noticeBo, "noticeTitle");
        }
        List<SysNotice> notices = getNoticesByConditions(noticeBo);
        if (notices.isEmpty()) {
            return AjaxResult.error("公告不存在");
        }
        if (notices.size() > 1) {
            return formMissing("查询到多条公告，请补充以下信息以精确定位：" + formatNoticeList(notices), noticeBo);
        }
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        sysNotice.setNoticeId(notices.getFirst().getNoticeId());
        R<Boolean> result = remoteNoticeService.updateNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeDelete(SysNoticeBo noticeBo) {
        if (noticeBo.getNoticeTitle() == null || noticeBo.getNoticeTitle().isEmpty()) {
            return formMissing("删除操作必须传入公告标题，请补充", noticeBo, "noticeTitle");
        }
        List<SysNotice> notices = getNoticesByConditions(noticeBo);
        if (notices.isEmpty()) {
            return AjaxResult.error("公告不存在");
        }
        if (notices.size() > 1) {
            return formMissing("查询到多条公告，请补充以下信息以精确定位：" + formatNoticeList(notices), noticeBo);
        }
        R<Boolean> result = remoteNoticeService.deleteNotice(notices.getFirst().getNoticeId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "岗位数据的新增、修改、删除。参数包含 operationType(add/update/delete)和岗位实体。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
    public AjaxResult postCrud(SysPostBo postBo) {
        AjaxResult gate = crudGate(postBo, "postCrud", "岗位", OPS_CRUD, "postName");
        if (gate != null) {
            return gate;
        }
        return executeWithErrorHandling(() -> switch (postBo.getOperationType().toLowerCase()) {
            case "add" -> handlePostAdd(postBo);
            case "update" -> handlePostUpdate(postBo);
            case "delete" -> handlePostDelete(postBo);
            default -> AjaxResult.error("不支持的操作类型：" + postBo.getOperationType() + "，请使用：add、update、delete");
        }, "岗位操作");
    }

    private AjaxResult handlePostAdd(SysPostBo postBo) {
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        R<Boolean> result = remotePostService.addPost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostUpdate(SysPostBo postBo) {
        if (postBo.getPostName() == null || postBo.getPostName().isEmpty()) {
            return formMissing("修改操作必须传入岗位名称，请补充", postBo, "postName");
        }
        List<SysPost> posts = getPostsByConditions(postBo);
        if (posts.isEmpty()) {
            return AjaxResult.error("岗位不存在");
        }
        if (posts.size() > 1) {
            return formMissing("查询到多个岗位，请补充以下信息以精确定位：" + formatPostList(posts), postBo);
        }
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        sysPost.setPostId(posts.getFirst().getPostId());
        R<Boolean> result = remotePostService.updatePost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostDelete(SysPostBo postBo) {
        if (postBo.getPostName() == null || postBo.getPostName().isEmpty()) {
            return formMissing("删除操作必须传入岗位名称，请补充", postBo, "postName");
        }
        List<SysPost> posts = getPostsByConditions(postBo);
        if (posts.isEmpty()) {
            return AjaxResult.error("岗位不存在");
        }
        if (posts.size() > 1) {
            return formMissing("查询到多个岗位，请补充以下信息以精确定位：" + formatPostList(posts), postBo);
        }
        R<Boolean> result = remotePostService.deletePost(posts.getFirst().getPostId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private String formatNoticeList(List<SysNotice> notices) {
        StringBuilder sb = new StringBuilder();
        for (SysNotice n : notices) {
            sb.append(String.format("[标题:%s, 类型:%s, 状态:%s] ", n.getNoticeTitle(), n.getNoticeType(),
                    "0".equals(n.getStatus()) ? "正常" : "关闭"));
        }
        return sb.toString();
    }

    private String formatPostList(List<SysPost> posts) {
        StringBuilder sb = new StringBuilder();
        for (SysPost p : posts) {
            sb.append(String.format("[岗位名称:%s, 编码:%s, 状态:%s] ", p.getPostName(), p.getPostCode(),
                    "0".equals(p.getStatus()) ? "正常" : "停用"));
        }
        return sb.toString();
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
