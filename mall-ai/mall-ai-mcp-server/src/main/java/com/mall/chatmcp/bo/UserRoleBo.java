package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;

public class UserRoleBo {

    @NotBlank(message = "用户账号不能为空")
    @JsonPropertyDescription("用户账号，例如 zhangsan")
    private String userName;

    @JsonPropertyDescription("角色名称列表，用于绑定用户的角色，例如 管理员、普通用户")
    private String[] roleNames;

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
}