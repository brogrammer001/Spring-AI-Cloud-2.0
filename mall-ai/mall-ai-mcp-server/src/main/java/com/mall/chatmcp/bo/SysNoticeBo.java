package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import com.mall.common.core.xss.Xss;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysNoticeBo extends BaseConfirmBo {

    @FieldRemark(name = "操作类型", readConverterExp = "add=新增,update=修改,delete=删除", excludeForm = true)
    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @FieldRemark(field = "noticeId", column = "notice_id", name = "公告ID", excludeForm = true)
    @JsonPropertyDescription("公告ID，选择记录后自动回传，无需手动填写")
    private Long noticeId;

    @FieldRemark(field = "noticeTitle", column = "notice_title", name = "公告标题")
    @Xss(message = "公告标题不能包含脚本字符")
    @NotBlank(message = "公告标题不能为空")
    @Size(min = 0, max = 50, message = "公告标题不能超过50个字符")
    @JsonPropertyDescription("公告标题")
    private String noticeTitle;

    @FieldRemark(field = "noticeType", column = "notice_type", name = "公告类型", readConverterExp = "1=通知,2=公告")
    @JsonPropertyDescription("公告类型，1通知 2公告")
    private String noticeType;

    @FieldRemark(field = "noticeContent", column = "notice_content", name = "公告内容")
    @JsonPropertyDescription("公告内容")
    private String noticeContent;

    @FieldRemark(field = "status", column = "status", name = "公告状态", readConverterExp = "0=正常,1=关闭", excludeForm = true)
    @JsonPropertyDescription("公告状态，0正常 1关闭")
    private String status;

    @FieldRemark(field = "remark", column = "remark", name = "备注")
    @JsonPropertyDescription("备注")
    private String remark;

    public Long getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(Long noticeId) {
        this.noticeId = noticeId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
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
