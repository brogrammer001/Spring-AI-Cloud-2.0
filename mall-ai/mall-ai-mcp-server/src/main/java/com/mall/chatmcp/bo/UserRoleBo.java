package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;

public class UserRoleBo {

    @NotNull(message = "用户ID不能为空")
    @JsonPropertyDescription("用户ID")
    private Long userId;

    @JsonPropertyDescription("角色ID列表，用于绑定用户的角色")
    private Long[] roleIds;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long[] getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Long[] roleIds) {
        this.roleIds = roleIds;
    }
}