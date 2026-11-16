package com.mall.chatmcp.bo;

import org.springframework.ai.tool.annotation.ToolParam;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysDictDataBo extends BaseConfirmBo {

    @FieldRemark(name = "操作类型", readConverterExp = "add=新增,update=修改,delete=删除", excludeForm = true)
    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    @ToolParam(required = false)
    private String operationType;

    @FieldRemark(field = "dictCode", column = "dict_code", name = "字典编码", excludeForm = true)
    @JsonPropertyDescription("字典编码，选择记录后自动回传，无需手动填写")
    @ToolParam(required = false)
    private Long dictCode;

    @FieldRemark(field = "dictLabel", column = "dict_label", name = "字典标签")
    @NotBlank(message = "字典标签不能为空")
    @Size(min = 0, max = 100, message = "字典标签长度不能超过100个字符")
    @JsonPropertyDescription("字典标签，例如 男、女、正常、停用")
    @ToolParam(required = false)
    private String dictLabel;

    @FieldRemark(field = "dictValue", column = "dict_value", name = "字典键值")
    @NotBlank(message = "字典键值不能为空")
    @Size(min = 0, max = 100, message = "字典键值长度不能超过100个字符")
    @JsonPropertyDescription("字典键值，例如 0、1、2")
    @ToolParam(required = false)
    private String dictValue;

    @FieldRemark(field = "dictType", column = "dict_type", name = "字典类型")
    @NotBlank(message = "字典类型不能为空")
    @Size(min = 0, max = 100, message = "字典类型长度不能超过100个字符")
    @JsonPropertyDescription("字典类型编码，例如 sys_user_sex、sys_job_status")
    @ToolParam(required = false)
    private String dictType;

    @FieldRemark(field = "dictSort", column = "dict_sort", name = "字典排序")
    @JsonPropertyDescription("字典排序序号")
    @ToolParam(required = false)
    private Long dictSort;

    @FieldRemark(field = "status", column = "status", name = "字典状态", readConverterExp = "0=正常,1=停用", excludeForm = true)
    @JsonPropertyDescription("字典状态：0正常 1停用")
    @ToolParam(required = false)
    private String status;

    @FieldRemark(field = "remark", column = "remark", name = "备注")
    @JsonPropertyDescription("备注")
    @ToolParam(required = false)
    private String remark;

    public Long getDictCode() {
        return dictCode;
    }

    public void setDictCode(Long dictCode) {
        this.dictCode = dictCode;
    }

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
