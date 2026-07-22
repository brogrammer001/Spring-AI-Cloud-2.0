package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;

public class RoleMenuBo {

    @NotBlank(message = "角色名称不能为空")
    @JsonPropertyDescription("角色名称，例如 管理员")
    private String roleName;

    @JsonPropertyDescription("菜单名称列表，用于绑定角色的菜单权限")
    private String[] menuNames;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String[] getMenuNames() {
        return menuNames;
    }

    public void setMenuNames(String[] menuNames) {
        this.menuNames = menuNames;
    }
}