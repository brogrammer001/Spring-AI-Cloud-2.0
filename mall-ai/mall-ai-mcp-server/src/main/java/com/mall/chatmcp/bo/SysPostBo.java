package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SysPostBo {

    @JsonPropertyDescription("岗位ID，修改和删除操作时必填")
    private Long postId;

    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除，query-查询")
    private String operationType;

    @NotBlank(message = "岗位编码不能为空")
    @Size(min = 0, max = 64, message = "岗位编码长度不能超过64个字符")
    @JsonPropertyDescription("岗位编码")
    private String postCode;

    @NotBlank(message = "岗位名称不能为空")
    @Size(min = 0, max = 50, message = "岗位名称长度不能超过50个字符")
    @JsonPropertyDescription("岗位名称，例如 经理、主管")
    private String postName;

    @NotNull(message = "显示顺序不能为空")
    @JsonPropertyDescription("岗位排序序号")
    private Integer postSort;

    @JsonPropertyDescription("岗位状态，0正常 1停用")
    private String status;

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