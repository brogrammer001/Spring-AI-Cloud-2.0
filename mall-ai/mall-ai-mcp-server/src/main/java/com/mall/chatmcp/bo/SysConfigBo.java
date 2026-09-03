package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysConfigBo {

    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @NotBlank(message = "参数名称不能为空")
    @Size(min = 0, max = 100, message = "参数名称不能超过100个字符")
    @JsonPropertyDescription("参数名称，例如 用户管理-账号初始密码")
    private String configName;

    @NotBlank(message = "参数键名不能为空")
    @Size(min = 0, max = 100, message = "参数键名长度不能超过100个字符")
    @JsonPropertyDescription("参数键名，例如 sys.user.initPassword")
    private String configKey;

    @NotBlank(message = "参数键值不能为空")
    @Size(min = 0, max = 500, message = "参数键值长度不能超过500个字符")
    @JsonPropertyDescription("参数键值，例如 123456")
    private String configValue;

    @JsonPropertyDescription("系统内置：Y是 N否")
    private String configType;

    @JsonPropertyDescription("备注")
    private String remark;

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}