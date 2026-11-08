package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;

public class UserPostBo extends BaseConfirmBo {

    @FieldRemark(field = "userName", column = "user_name", name = "用户账号")
    @NotBlank(message = "用户账号不能为空")
    @JsonPropertyDescription("用户账号，例如 zhangsan")
    private String userName;

    @FieldRemark(field = "postIds", column = "post_id", name = "岗位名称列表")
    @JsonPropertyDescription("岗位名称列表，用于绑定用户的岗位，例如 经理、主管")
    private String[] postNames;

    @FieldRemark(field = "userId", column = "user_id", name = "用户ID", excludeForm = true)
    @JsonPropertyDescription("用户ID，选择用户后自动回传，无需手动填写")
    private Long userId;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String[] getPostNames() {
        return postNames;
    }

    public void setPostNames(String[] postNames) {
        this.postNames = postNames;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
