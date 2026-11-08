package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.mall.common.core.annotation.FieldRemark;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SysPostBo extends BaseConfirmBo {

    @FieldRemark(name = "操作类型", readConverterExp = "add=新增,update=修改,delete=删除", excludeForm = true)
    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除")
    private String operationType;

    @FieldRemark(field = "postId", column = "post_id", name = "岗位ID", excludeForm = true)
    @JsonPropertyDescription("岗位ID，选择记录后自动回传，无需手动填写")
    private Long postId;

    @FieldRemark(field = "postCode", column = "post_code", name = "岗位编码")
    @NotBlank(message = "岗位编码不能为空")
    @Size(min = 0, max = 64, message = "岗位编码长度不能超过64个字符")
    @JsonPropertyDescription("岗位编码")
    private String postCode;

    @FieldRemark(field = "postName", column = "post_name", name = "岗位名称")
    @NotBlank(message = "岗位名称不能为空")
    @Size(min = 0, max = 50, message = "岗位名称长度不能超过50个字符")
    @JsonPropertyDescription("岗位名称，例如 经理、主管")
    private String postName;

    @FieldRemark(field = "postSort", column = "post_sort", name = "显示顺序")
    @NotNull(message = "显示顺序不能为空")
    @JsonPropertyDescription("岗位排序序号")
    private Integer postSort;

    @FieldRemark(field = "status", column = "status", name = "岗位状态", readConverterExp = "0=正常,1=停用", excludeForm = true)
    @JsonPropertyDescription("岗位状态，0正常 1停用")
    private String status;

    @FieldRemark(field = "remark", column = "remark", name = "备注")
    @JsonPropertyDescription("备注")
    private String remark;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public Integer getPostSort() {
        return postSort;
    }

    public void setPostSort(Integer postSort) {
        this.postSort = postSort;
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
