package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;

public class UserDeptBo {

    @NotNull(message = "用户ID不能为空")
    @JsonPropertyDescription("用户ID")
    private Long userId;

    @JsonPropertyDescription("部门ID，用于修改用户所属部门（可选，不传则查询当前部门）")
    private Long deptId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}