package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;

public class RoleDeptBo {

    @NotBlank(message = "角色名称不能为空")
    @JsonPropertyDescription("角色名称，例如 管理员")
    private String roleName;

    @JsonPropertyDescription("部门名称列表，用于绑定角色的数据权限范围，例如 财务部、研发部")
    private String[] deptNames;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String[] getDeptNames() {
        return deptNames;
    }

    public void setDeptNames(String[] deptNames) {
        this.deptNames = deptNames;
    }
}