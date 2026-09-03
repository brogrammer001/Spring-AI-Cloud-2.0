package com.mall.chatmcp.bo;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SysJobBo {

    @JsonPropertyDescription("操作类型：add-新增，update-修改，delete-删除，changeStatus-修改状态，run-立即执行")
    private String operationType;

    @NotBlank(message = "任务名称不能为空")
    @Size(min = 0, max = 64, message = "任务名称不能超过64个字符")
    @JsonPropertyDescription("任务名称，例如 系统默认（无参）")
    private String jobName;

    @JsonPropertyDescription("任务组名，例如 DEFAULT、SYSTEM")
    private String jobGroup;

    @NotBlank(message = "调用目标字符串不能为空")
    @Size(min = 0, max = 500, message = "调用目标字符串长度不能超过500个字符")
    @JsonPropertyDescription("调用目标字符串，例如 ryTask.ryNoParams")
    private String invokeTarget;

    @NotBlank(message = "Cron执行表达式不能为空")
    @Size(min = 0, max = 255, message = "Cron执行表达式不能超过255个字符")
    @JsonPropertyDescription("Cron执行表达式，例如 0/10 * * * * ?")
    private String cronExpression;

    @JsonPropertyDescription("计划执行错误策略：1立即执行 2执行一次 3放弃执行")
    private String misfirePolicy;

    @JsonPropertyDescription("是否并发执行：0允许 1禁止")
    private String concurrent;

    @JsonPropertyDescription("任务状态：0正常 1暂停")
    private String status;

    @JsonPropertyDescription("备注")
    private String remark;

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getInvokeTarget() {
        return invokeTarget;
    }

    public void setInvokeTarget(String invokeTarget) {
        this.invokeTarget = invokeTarget;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getMisfirePolicy() {
        return misfirePolicy;
    }

    public void setMisfirePolicy(String misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    public String getConcurrent() {
        return concurrent;
    }

    public void setConcurrent(String concurrent) {
        this.concurrent = concurrent;
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