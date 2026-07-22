package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SysRoleBo {

    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除，query-查询")
    private String operationType;

    @JsonPropertyDescription("角色ID，修改和删除操作时必填")
    private Long roleId;

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 0, max = 30, message = "角色名称长度不能超过30个字符")
    @JsonPropertyDescription("角色名称，例如 管理员、普通用户")
    private String roleName;

    @NotBlank(message = "权限字符不能为空")
    @Size(min = 0, max = 100, message = "权限字符长度不能超过100个字符")
    @JsonPropertyDescription("角色权限标识，例如 admin")
    private String roleKey;

    @NotNull(message = "显示顺序不能为空")
    @JsonPropertyDescription("角色排序序号")
    private Integer roleSort;

    @JsonPropertyDescription("数据范围：1所有数据权限，2自定义数据权限，3本部门数据权限，4本部门及以下数据权限，5仅本人数据权限")
    private String dataScope;

    @JsonPropertyDescription("角色状态，0正常 1停用")
    private String status;

    @JsonPropertyDescription("备注")
    private String remark;

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public Integer getRoleSort() {
        return roleSort;
    }

    public void setRoleSort(Integer roleSort) {
        this.roleSort = roleSort;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}