package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;

public class RoleMenuBo extends BaseConfirmBo {

    @FieldRemark(field = "roleName", column = "role_name", name = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @JsonPropertyDescription("角色名称，例如 管理员")
    private String roleName;

    @FieldRemark(field = "menuIds", column = "menu_id", name = "菜单名称列表")
    @JsonPropertyDescription("菜单名称列表，用于绑定角色的菜单权限")
    private String[] menuNames;

    @FieldRemark(field = "roleId", column = "role_id", name = "角色ID", excludeForm = true)
    @JsonPropertyDescription("角色ID，选择角色后自动回传，无需手动填写")
    private Long roleId;

    @FieldRemark(field = "menuId", column = "menu_id", name = "菜单ID", excludeForm = true)
    @JsonPropertyDescription("菜单ID，选择菜单后自动回传，无需手动填写")
    private Long menuId;

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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
