package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class RoleDeptBo {

    @NotNull(message = "角色ID不能为空")
    @JsonPropertyDescription("角色ID")
    private Long roleId;

    @JsonPropertyDescription("部门ID列表，用于绑定角色的数据权限范围")
    private Long[] deptIds;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long[] getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(Long[] deptIds) {
        this.deptIds = deptIds;
    }
}