package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;

public class RoleMenuBo {

    @NotNull(message = "角色ID不能为空")
    @JsonPropertyDescription("角色ID")
    private Long roleId;

    @JsonPropertyDescription("菜单ID列表，用于绑定角色的菜单权限")
    private Long[] menuIds;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long[] getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(Long[] menuIds) {
        this.menuIds = menuIds;
    }
}