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
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Tool(description = "通知公告的新增、修改、删除。参数包含 operationType(add/update/delete)和公告实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
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
        Long noticeId = noticeBo.getNoticeId();
        if (noticeId == null) {
            if (noticeBo.getNoticeTitle() == null || noticeBo.getNoticeTitle().isEmpty()) {
                return formMissing("修改操作必须传入公告标题，请补充", noticeBo, "noticeTitle");
            }
            List<SysNotice> notices = getNoticesByConditions(noticeBo);
            if (notices.isEmpty()) {
                return AjaxResult.error("公告不存在");
            }
            if (notices.size() > 1) {
                return formSelect("查询到 " + notices.size() + " 条公告，请选择要修改的公告：",
                    noticeBo, notices, SysNotice::getNoticeId, this::noticeLabel, "noticeId", "公告");
            }
            noticeId = notices.getFirst().getNoticeId();
        }
        SysNotice sysNotice = new SysNotice();
        BeanUtils.copyProperties(noticeBo, sysNotice);
        sysNotice.setNoticeId(noticeId);
        R<Boolean> result = remoteNoticeService.updateNotice(sysNotice);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleNoticeDelete(SysNoticeBo noticeBo) {
        Long noticeId = noticeBo.getNoticeId();
        if (noticeId == null) {
            if (noticeBo.getNoticeTitle() == null || noticeBo.getNoticeTitle().isEmpty()) {
                return formMissing("删除操作必须传入公告标题，请补充", noticeBo, "noticeTitle");
            }
            List<SysNotice> notices = getNoticesByConditions(noticeBo);
            if (notices.isEmpty()) {
                return AjaxResult.error("公告不存在");
            }
            if (notices.size() > 1) {
                return formSelect("查询到 " + notices.size() + " 条公告，请选择要删除的公告：",
                    noticeBo, notices, SysNotice::getNoticeId, this::noticeLabel, "noticeId", "公告");
            }
            noticeId = notices.getFirst().getNoticeId();
        }
        R<Boolean> result = remoteNoticeService.deleteNotice(noticeId);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    @Tool(description = "岗位数据的新增、修改、删除。参数包含 operationType(add/update/delete)和岗位实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
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
        Long postId = postBo.getPostId();
        if (postId == null) {
            if (postBo.getPostName() == null || postBo.getPostName().isEmpty()) {
                return formMissing("修改操作必须传入岗位名称，请补充", postBo, "postName");
            }
            List<SysPost> posts = getPostsByConditions(postBo);
            if (posts.isEmpty()) {
                return AjaxResult.error("岗位不存在");
            }
            if (posts.size() > 1) {
                return formSelect("查询到 " + posts.size() + " 个岗位，请选择要修改的岗位：",
                    postBo, posts, SysPost::getPostId, this::postLabel, "postId", "岗位");
            }
            postId = posts.getFirst().getPostId();
        }
        SysPost sysPost = new SysPost();
        BeanUtils.copyProperties(postBo, sysPost);
        sysPost.setPostId(postId);
        R<Boolean> result = remotePostService.updatePost(sysPost);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handlePostDelete(SysPostBo postBo) {
        Long postId = postBo.getPostId();
        if (postId == null) {
            if (postBo.getPostName() == null || postBo.getPostName().isEmpty()) {
                return formMissing("删除操作必须传入岗位名称，请补充", postBo, "postName");
            }
            List<SysPost> posts = getPostsByConditions(postBo);
            if (posts.isEmpty()) {
                return AjaxResult.error("岗位不存在");
            }
            if (posts.size() > 1) {
                return formSelect("查询到 " + posts.size() + " 个岗位，请选择要删除的岗位：",
                    postBo, posts, SysPost::getPostId, this::postLabel, "postId", "岗位");
            }
            postId = posts.getFirst().getPostId();
        }
        R<Boolean> result = remotePostService.deletePost(postId);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private String noticeLabel(SysNotice n) {
        return "标题：" + n.getNoticeTitle()
            + "，类型：" + n.getNoticeType()
            + "，状态：" + ("0".equals(n.getStatus()) ? "正常" : "关闭");
    }

    private String postLabel(SysPost p) {
        return "岗位名称：" + p.getPostName()
            + "，编码：" + p.getPostCode()
            + "，状态：" + ("0".equals(p.getStatus()) ? "正常" : "停用");
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

    @Tool(description = "批量新增通知公告。参数：公告列表(noticeList)，每个元素需包含公告标题、公告类型、公告内容等必填信息。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddNotices(
            @ToolParam(description = "公告信息列表") List<SysNoticeBo> noticeList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(noticeList, "公告");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysNoticeBo noticeBo : noticeList) {
                AjaxResult validateResult = validate(noticeBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("noticeList", noticeList.stream().map(this::toMap).toList());
                return confirmResult("batchAddNotices", "add", "确认批量新增公告",
                    "即将批量新增 " + noticeList.size() + " 条公告，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下公告新增失败：");
            for (SysNoticeBo noticeBo : noticeList) {
                SysNotice sysNotice = new SysNotice();
                BeanUtils.copyProperties(noticeBo, sysNotice);
                R<Boolean> result = remoteNoticeService.addNotice(sysNotice);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(noticeBo.getNoticeTitle());
                } else {
                    batch.addFail(noticeBo.getNoticeTitle(), result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增公告");
    }

    @Tool(description = "批量删除通知公告。参数：公告标题列表(noticeTitles)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeleteNotices(
            @ToolParam(description = "公告标题列表") List<String> noticeTitles,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(noticeTitles, "公告");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("noticeTitles", noticeTitles);
                return confirmResult("batchDeleteNotices", "delete", "确认批量删除公告",
                    "即将批量删除 " + noticeTitles.size() + " 条公告：" + joinNames(noticeTitles) + "，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下公告删除失败：");
            for (String noticeTitle : noticeTitles) {
                SysNoticeBo query = new SysNoticeBo();
                query.setNoticeTitle(noticeTitle);
                List<SysNotice> notices = getNoticesByConditions(query);
                if (notices.isEmpty()) {
                    batch.addFail(noticeTitle, "公告不存在");
                    continue;
                }
                if (notices.size() > 1) {
                    batch.addFail(noticeTitle, "匹配到多条记录，请使用更精确的公告标题或先查询公告ID");
                    continue;
                }
                R<Boolean> result = remoteNoticeService.deleteNotice(notices.getFirst().getNoticeId());
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(noticeTitle);
                } else {
                    batch.addFail(noticeTitle, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除公告");
    }

    @Tool(description = "批量修改通知公告。参数：公告列表(noticeList)，每个元素需包含公告标题(noticeTitle)或公告ID(noticeId)用于定位，另可填写公告类型、公告内容、状态、备注等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdateNotices(
            @ToolParam(description = "公告信息列表") List<SysNoticeBo> noticeList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(noticeList, "公告");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("noticeList", noticeList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdateNotices", "update", "确认批量修改公告",
                    "即将批量修改 " + noticeList.size() + " 条公告，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下公告修改失败：");
            for (SysNoticeBo noticeBo : noticeList) {
                String label = noticeBo.getNoticeTitle() != null && !noticeBo.getNoticeTitle().isEmpty()
                    ? noticeBo.getNoticeTitle() : "ID=" + noticeBo.getNoticeId();
                Long noticeId = noticeBo.getNoticeId();
                if (noticeId == null) {
                    if (noticeBo.getNoticeTitle() == null || noticeBo.getNoticeTitle().isEmpty()) {
                        batch.addFail(label, "缺少公告标题或公告ID");
                        continue;
                    }
                    List<SysNotice> notices = getNoticesByConditions(noticeBo);
                    if (notices.isEmpty()) {
                        batch.addFail(label, "公告不存在");
                        continue;
                    }
                    if (notices.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带公告ID");
                        continue;
                    }
                    noticeId = notices.getFirst().getNoticeId();
                }

                SysNotice sysNotice = new SysNotice();
                BeanUtils.copyProperties(noticeBo, sysNotice);
                sysNotice.setNoticeId(noticeId);
                R<Boolean> result = remoteNoticeService.updateNotice(sysNotice);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改公告");
    }

    @Tool(description = "批量新增岗位。参数：岗位列表(postList)，每个元素需包含岗位名称、岗位编码等必填信息，可填写排序、状态、备注。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddPosts(
            @ToolParam(description = "岗位信息列表") List<SysPostBo> postList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(postList, "岗位");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysPostBo postBo : postList) {
                AjaxResult validateResult = validate(postBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("postList", postList.stream().map(this::toMap).toList());
                return confirmResult("batchAddPosts", "add", "确认批量新增岗位",
                    "即将批量新增 " + postList.size() + " 个岗位，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下岗位新增失败：");
            for (SysPostBo postBo : postList) {
                SysPost sysPost = new SysPost();
                BeanUtils.copyProperties(postBo, sysPost);
                R<Boolean> result = remotePostService.addPost(sysPost);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(postBo.getPostName());
                } else {
                    batch.addFail(postBo.getPostName(), result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增岗位");
    }

    @Tool(description = "批量删除岗位。参数：岗位名称列表(postNames)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeletePosts(
            @ToolParam(description = "岗位名称列表，例如 [\"经理\",\"主管\"]") List<String> postNames,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(postNames, "岗位");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("postNames", postNames);
                return confirmResult("batchDeletePosts", "delete", "确认批量删除岗位",
                    "即将批量删除 " + postNames.size() + " 个岗位：" + joinNames(postNames) + "，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下岗位删除失败：");
            for (String postName : postNames) {
                SysPostBo query = new SysPostBo();
                query.setPostName(postName);
                List<SysPost> posts = getPostsByConditions(query);
                if (posts.isEmpty()) {
                    batch.addFail(postName, "岗位不存在");
                    continue;
                }
                if (posts.size() > 1) {
                    batch.addFail(postName, "匹配到多条记录，请使用更精确的岗位名称或先查询岗位ID");
                    continue;
                }
                R<Boolean> result = remotePostService.deletePost(posts.getFirst().getPostId());
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(postName);
                } else {
                    batch.addFail(postName, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除岗位");
    }

    @Tool(description = "批量修改岗位。参数：岗位列表(postList)，每个元素需包含岗位名称(postName)或岗位ID(postId)用于定位，另可填写岗位编码、排序、状态、备注等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdatePosts(
            @ToolParam(description = "岗位信息列表") List<SysPostBo> postList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(postList, "岗位");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("postList", postList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdatePosts", "update", "确认批量修改岗位",
                    "即将批量修改 " + postList.size() + " 个岗位，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下岗位修改失败：");
            for (SysPostBo postBo : postList) {
                String label = postBo.getPostName() != null && !postBo.getPostName().isEmpty()
                    ? postBo.getPostName() : "ID=" + postBo.getPostId();
                Long postId = postBo.getPostId();
                if (postId == null) {
                    if (postBo.getPostName() == null || postBo.getPostName().isEmpty()) {
                        batch.addFail(label, "缺少岗位名称或岗位ID");
                        continue;
                    }
                    List<SysPost> posts = getPostsByConditions(postBo);
                    if (posts.isEmpty()) {
                        batch.addFail(label, "岗位不存在");
                        continue;
                    }
                    if (posts.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带岗位ID");
                        continue;
                    }
                    postId = posts.getFirst().getPostId();
                }

                SysPost sysPost = new SysPost();
                BeanUtils.copyProperties(postBo, sysPost);
                sysPost.setPostId(postId);
                R<Boolean> result = remotePostService.updatePost(sysPost);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改岗位");
    }
}
