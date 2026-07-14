package com.mall.aichat.config;

import com.mall.aichat.domain.ChatEventType;
import org.springframework.stereotype.Component;

@Component
public class ToolResultHolder {

    private ChatEventType actionType;   // OPEN_MENU / DATA_TABLE / CONFIRM
    private Object actionData;   // 工具返回的数据
    private boolean hasAction; // 是否需要用户确认
    private String confirmDesc;  // 确认描述文本


    public ChatEventType getActionType() {
        return actionType;
    }

    public void setActionType(ChatEventType actionType) {
        this.actionType = actionType;
    }

    public Object getActionData() {
        return actionData;
    }

    public void setActionData(Object actionData) {
        this.actionData = actionData;
    }

    public boolean isHasAction() {
        return hasAction;
    }

    public void setHasAction(boolean hasAction) {
        this.hasAction = hasAction;
    }

    public String getConfirmDesc() {
        return confirmDesc;
    }

    public void setConfirmDesc(String confirmDesc) {
        this.confirmDesc = confirmDesc;
    }

    public void clear() {
        hasAction = false;
    }
}
