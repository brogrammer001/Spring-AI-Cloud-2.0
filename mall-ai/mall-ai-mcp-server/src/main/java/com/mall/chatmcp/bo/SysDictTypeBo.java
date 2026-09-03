package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysDictTypeBo {

    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @NotBlank(message = "字典名称不能为空")
    @Size(min = 0, max = 100, message = "字典名称长度不能超过100个字符")
    @JsonPropertyDescription("字典名称，例如 用户性别、任务状态")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Size(min = 0, max = 100, message = "字典类型长度不能超过100个字符")
    @JsonPropertyDescription("字典类型编码，例如 sys_user_sex、sys_job_status")
    private String dictType;

    @JsonPropertyDescription("字典状态：0正常 1停用")
    private String status;

    @JsonPropertyDescription("备注")
    private String remark;

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
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