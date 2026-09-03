package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysDictDataBo {

    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @NotBlank(message = "字典标签不能为空")
    @Size(min = 0, max = 100, message = "字典标签长度不能超过100个字符")
    @JsonPropertyDescription("字典标签，例如 男、女、正常、停用")
    private String dictLabel;

    @NotBlank(message = "字典键值不能为空")
    @Size(min = 0, max = 100, message = "字典键值长度不能超过100个字符")
    @JsonPropertyDescription("字典键值，例如 0、1、2")
    private String dictValue;

    @NotBlank(message = "字典类型不能为空")
    @Size(min = 0, max = 100, message = "字典类型长度不能超过100个字符")
    @JsonPropertyDescription("字典类型编码，例如 sys_user_sex、sys_job_status")
    private String dictType;

    @JsonPropertyDescription("字典排序序号")
    private Long dictSort;

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

    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    public String getDictValue() {
        return dictValue;
    }

    public void setDictValue(String dictValue) {
        this.dictValue = dictValue;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public Long getDictSort() {
        return dictSort;
    }

    public void setDictSort(Long dictSort) {
        this.dictSort = dictSort;
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