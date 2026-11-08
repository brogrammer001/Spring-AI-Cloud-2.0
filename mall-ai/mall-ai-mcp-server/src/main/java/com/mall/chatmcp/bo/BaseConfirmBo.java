package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;

/**
 * 变更类工具参数基类：携带用户二次确认标志
 */
public class BaseConfirmBo {

    @FieldRemark(name = "用户确认标志", excludeForm = true)
    @JsonPropertyDescription("用户确认标志：用户在前端点击同意按钮后为true。未确认的变更操作会返回确认提示，AI不要自行设置该值")
    private Boolean confirmed;

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }
}
