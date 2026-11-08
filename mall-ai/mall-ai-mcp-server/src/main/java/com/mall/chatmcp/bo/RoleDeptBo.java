package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;

public class RoleDeptBo extends BaseConfirmBo {

    @FieldRemark(field = "roleName", column = "role_name", name = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @JsonPropertyDescription("角色名称，例如 管理员")
    private String roleName;

    @FieldRemark(field = "deptIds", column = "dept_id", name = "部门名称列表")
    @JsonPropertyDescription("部门名称列表，用于绑定角色的数据权限范围，例如 财务部、研发部")
    private String[] deptNames;

    @FieldRemark(field = "roleId", column = "role_id", name = "角色ID", excludeForm = true)
    @JsonPropertyDescription("角色ID，选择角色后自动回传，无需手动填写")
    private Long roleId;

    @FieldRemark(field = "deptId", column = "dept_id", name = "部门ID", excludeForm = true)
    @JsonPropertyDescription("部门ID，选择部门后自动回传，无需手动填写")
    private Long deptId;

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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}
