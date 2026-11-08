package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SysDeptBo extends BaseConfirmBo {

    @FieldRemark(name = "操作类型", readConverterExp = "add=新增,update=修改,delete=删除", excludeForm = true)
    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @FieldRemark(field = "deptId", column = "dept_id", name = "部门ID", excludeForm = true)
    @JsonPropertyDescription("部门ID，选择记录后自动回传，无需手动填写")
    private Long deptId;

    @FieldRemark(field = "parentId", column = "parent_id", name = "父部门ID", excludeForm = true)
    @JsonPropertyDescription("父部门ID，选择父部门后自动回传，无需手动填写")
    private Long parentId;

    @FieldRemark(field = "parentName", name = "父部门名称")
    @JsonPropertyDescription("父部门名称，为空则为顶级部门")
    private String parentName;

    @FieldRemark(field = "deptName", column = "dept_name", name = "部门名称")
    @NotBlank(message = "部门名称不能为空")
    @Size(min = 0, max = 30, message = "部门名称长度不能超过30个字符")
    @JsonPropertyDescription("部门名称，例如 财务部、研发部")
    private String deptName;

    @FieldRemark(field = "orderNum", column = "order_num", name = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    @JsonPropertyDescription("显示顺序")
    private Integer orderNum;

    @FieldRemark(field = "leader", column = "leader", name = "负责人")
    @JsonPropertyDescription("负责人")
    private String leader;

    @FieldRemark(field = "phone", column = "phone", name = "联系电话")
    @Size(min = 0, max = 11, message = "联系电话长度不能超过11个字符")
    @JsonPropertyDescription("联系电话")
    private String phone;

    @FieldRemark(field = "email", column = "email", name = "邮箱")
    @JsonPropertyDescription("邮箱")
    private String email;

    @FieldRemark(field = "status", column = "status", name = "部门状态", readConverterExp = "0=正常,1=停用", excludeForm = true)
    @JsonPropertyDescription("部门状态，0正常 1停用")
    private String status;

    @FieldRemark(field = "remark", column = "remark", name = "备注")
    @JsonPropertyDescription("备注")
    private String remark;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
