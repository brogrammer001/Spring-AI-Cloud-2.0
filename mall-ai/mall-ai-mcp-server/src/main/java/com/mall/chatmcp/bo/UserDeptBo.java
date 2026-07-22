package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;

public class UserDeptBo {

    @NotBlank(message = "用户账号不能为空")
    @JsonPropertyDescription("用户账号，例如 zhangsan")
    private String userName;

    @JsonPropertyDescription("部门名称，用于修改用户所属部门（可选，不传则查询当前部门），例如 财务部")
    private String deptName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}