package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;

public class UserRoleBo extends BaseConfirmBo {

    @FieldRemark(field = "userName", column = "user_name", name = "用户账号")
    @NotBlank(message = "用户账号不能为空")
    @JsonPropertyDescription("用户账号，例如 zhangsan")
    private String userName;

    @FieldRemark(field = "roleIds", column = "role_id", name = "角色名称列表")
    @JsonPropertyDescription("角色名称列表，用于绑定用户的角色，例如 管理员、普通用户")
    private String[] roleNames;

    @FieldRemark(field = "userId", column = "user_id", name = "用户ID", excludeForm = true)
    @JsonPropertyDescription("用户ID，选择用户后自动回传，无需手动填写")
    private Long userId;

    @FieldRemark(field = "roleId", column = "role_id", name = "角色ID", excludeForm = true)
    @JsonPropertyDescription("角色ID，选择角色后自动回传，无需手动填写")
    private Long roleId;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String[] getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(String[] roleNames) {
        this.roleNames = roleNames;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
