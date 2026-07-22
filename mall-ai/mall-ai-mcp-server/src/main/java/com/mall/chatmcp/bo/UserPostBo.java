package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;

public class UserPostBo {

    @NotBlank(message = "用户账号不能为空")
    @JsonPropertyDescription("用户账号，例如 zhangsan")
    private String userName;

    @JsonPropertyDescription("岗位名称列表，用于绑定用户的岗位，例如 经理、主管")
    private String[] postNames;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String[] getPostNames() {
        return postNames;
    }

    public void setPostNames(String[] postNames) {
        this.postNames = postNames;
    }
}