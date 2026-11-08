package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SysRoleBo extends BaseConfirmBo {

    @FieldRemark(name = "操作类型", readConverterExp = "add=新增,update=修改,delete=删除", excludeForm = true)
    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @FieldRemark(field = "roleId", column = "role_id", name = "角色ID", excludeForm = true)
    @JsonPropertyDescription("角色ID，选择记录后自动回传，无需手动填写")
    private Long roleId;

    @FieldRemark(field = "roleName", column = "role_name", name = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 0, max = 30, message = "角色名称长度不能超过30个字符")
    @JsonPropertyDescription("角色名称，例如 管理员、普通用户")
    private String roleName;

    @FieldRemark(field = "roleKey", column = "role_key", name = "角色权限标识")
    @NotBlank(message = "权限字符不能为空")
    @Size(min = 0, max = 100, message = "权限字符长度不能超过100个字符")
    @JsonPropertyDescription("角色权限标识，例如 admin")
    private String roleKey;

    @FieldRemark(field = "roleSort", column = "role_sort", name = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    @JsonPropertyDescription("角色排序序号")
    private Integer roleSort;

    @FieldRemark(field = "dataScope", column = "data_scope", name = "数据范围", readConverterExp = "1=所有数据权限,2=自定义数据权限,3=本部门数据权限,4=本部门及以下数据权限,5=仅本人数据权限")
    @JsonPropertyDescription("数据范围：1所有数据权限，2自定义数据权限，3本部门数据权限，4本部门及以下数据权限，5仅本人数据权限")
    private String dataScope;

    @FieldRemark(field = "status", column = "status", name = "角色状态", readConverterExp = "0=正常,1=停用", excludeForm = true)
    @JsonPropertyDescription("角色状态，0正常 1停用")
    private String status;

    @FieldRemark(field = "remark", column = "remark", name = "备注")
    @JsonPropertyDescription("备注")
    private String remark;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
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
