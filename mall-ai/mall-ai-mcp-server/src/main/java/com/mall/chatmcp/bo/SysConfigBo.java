package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysConfigBo extends BaseConfirmBo {

    @FieldRemark(name = "操作类型", readConverterExp = "add=新增,update=修改,delete=删除", excludeForm = true)
    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @FieldRemark(field = "configId", column = "config_id", name = "参数配置ID", excludeForm = true)
    @JsonPropertyDescription("参数配置ID，选择记录后自动回传，无需手动填写")
    private Long configId;

    @FieldRemark(field = "configName", column = "config_name", name = "参数名称")
    @NotBlank(message = "参数名称不能为空")
    @Size(min = 0, max = 100, message = "参数名称不能超过100个字符")
    @JsonPropertyDescription("参数名称，例如 用户管理-账号初始密码")
    private String configName;

    @FieldRemark(field = "configKey", column = "config_key", name = "参数键名")
    @NotBlank(message = "参数键名不能为空")
    @Size(min = 0, max = 100, message = "参数键名长度不能超过100个字符")
    @JsonPropertyDescription("参数键名，例如 sys.user.initPassword")
    private String configKey;

    @FieldRemark(field = "configValue", column = "config_value", name = "参数键值")
    @NotBlank(message = "参数键值不能为空")
    @Size(min = 0, max = 500, message = "参数键值长度不能超过500个字符")
    @JsonPropertyDescription("参数键值，例如 123456")
    private String configValue;

    @FieldRemark(field = "configType", column = "config_type", name = "系统内置", readConverterExp = "Y=是,N=否")
    @JsonPropertyDescription("系统内置：Y是 N否")
    private String configType;

    @FieldRemark(field = "remark", column = "remark", name = "备注")
    @JsonPropertyDescription("备注")
    private String remark;

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

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
