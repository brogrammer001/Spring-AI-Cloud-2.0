package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;

public class UserPostBo {

    @NotNull(message = "用户ID不能为空")
    @JsonPropertyDescription("用户ID")
    private Long userId;

    @JsonPropertyDescription("岗位ID列表，用于绑定用户的岗位")
    private Long[] postIds;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long[] getPostIds() {
        return postIds;
    }

    public void setPostIds(Long[] postIds) {
        this.postIds = postIds;
    }
}