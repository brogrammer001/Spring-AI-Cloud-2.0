package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.xss.Xss;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysNoticeBo {

    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除，query-查询")
    private String operationType;

    @JsonPropertyDescription("公告ID，修改和删除操作时必填")
    private Long noticeId;

    @Xss(message = "公告标题不能包含脚本字符")
    @NotBlank(message = "公告标题不能为空")
    @Size(min = 0, max = 50, message = "公告标题不能超过50个字符")
    @JsonPropertyDescription("公告标题")
    private String noticeTitle;

    @JsonPropertyDescription("公告类型，1通知 2公告")
    private String noticeType;

    @JsonPropertyDescription("公告内容")
    private String noticeContent;

    @JsonPropertyDescription("公告状态，0正常 1关闭")
    private String status;

    @JsonPropertyDescription("备注")
    private String remark;

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    public String getNoticeTitle() {
        return noticeTitle;
    }

    public void setNoticeTitle(String noticeTitle) {
        this.noticeTitle = noticeTitle;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(String noticeType) {
        this.noticeType = noticeType;
    }

    public String getNoticeContent() {
        return noticeContent;
    }

    public void setNoticeContent(String noticeContent) {
        this.noticeContent = noticeContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}