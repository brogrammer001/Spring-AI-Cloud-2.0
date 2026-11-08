package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;

public class UserDeptBo extends BaseConfirmBo {

    @FieldRemark(field = "userName", column = "user_name", name = "用户账号")
    @NotBlank(message = "用户账号不能为空")
    @JsonPropertyDescription("用户账号，例如 zhangsan")
    private String userName;

    @FieldRemark(field = "userId", column = "user_id", name = "用户ID", excludeForm = true)
    @JsonPropertyDescription("用户ID，选择记录后自动回传，无需手动填写")
    private Long userId;

    @FieldRemark(field = "deptName", column = "dept_name", name = "部门名称")
    @JsonPropertyDescription("部门名称，用于修改用户所属部门（可选，不传则查询当前部门），例如 财务部")
    private String deptName;

    @FieldRemark(field = "deptId", column = "dept_id", name = "部门ID", excludeForm = true)
    @JsonPropertyDescription("部门ID，选择部门后自动回传，无需手动填写")
    private Long deptId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}
