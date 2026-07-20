package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SysDeptBo {

    @JsonPropertyDescription("部门ID，修改和删除操作时必填")
    private Long deptId;

    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除，query-查询")
    private String operationType;

    @JsonPropertyDescription("父部门ID")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(min = 0, max = 30, message = "部门名称长度不能超过30个字符")
    @JsonPropertyDescription("部门名称，例如 财务部、研发部")
    private String deptName;

    @NotNull(message = "显示顺序不能为空")
    @JsonPropertyDescription("显示顺序")
    private Integer orderNum;

    @JsonPropertyDescription("负责人")
    private String leader;

    @Size(min = 0, max = 11, message = "联系电话长度不能超过11个字符")
    @JsonPropertyDescription("联系电话")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    @JsonPropertyDescription("邮箱")
    private String email;

    @JsonPropertyDescription("部门状态，0正常 1停用")
    private String status;

    @JsonPropertyDescription("备注")
    private String remark;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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