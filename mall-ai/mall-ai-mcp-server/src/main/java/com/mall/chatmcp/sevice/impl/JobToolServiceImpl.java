package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysJobBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteJobService;
import com.mall.system.api.domain.SysJob;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.List;

@Service
public class JobToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteJobService remoteJobService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "定时任务的新增、修改、删除、状态修改、立即执行。参数包含 operationType(add/update/delete/changeStatus/run)和任务实体。首次调用会返回确认信息，用户点击同意后需携带 confirmed=true 再次调用。 [JSON]", returnDirect = true)
    public AjaxResult jobCrud(SysJobBo jobBo) {
        String operationType = jobBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return operationForm(jobBo, new String[][]{
                {"add", "新增"}, {"update", "修改"}, {"delete", "删除"},
                {"changestatus", "修改状态"}, {"run", "立即执行"}});
        }
        // 先补齐必填字段（表单），再要求用户确认
        switch (operationType.toLowerCase()) {
            case "add" -> {
                AjaxResult validateResult = validate(jobBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            case "update", "delete", "run" -> {
                if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
                    return formMissing(opLabel(operationType) + "操作必须传入任务名称，请补充", jobBo, "jobName");
                }
            }
            case "changestatus" -> {
                if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
                    return formMissing("状态修改操作必须传入任务名称，请补充", jobBo, "jobName");
                }
                if (jobBo.getStatus() == null || jobBo.getStatus().isEmpty()) {
                    return formMissing("状态修改操作必须传入任务状态：0正常 1暂停，请补充", jobBo, "status");
                }
            }
            default -> {
                return AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete、changeStatus、run");
            }
        }
        if (!isConfirmed(jobBo)) {
            return confirmCrud("jobCrud", "定时任务", operationType, toMap(jobBo));
        }

        return executeWithErrorHandling(() -> switch (operationType.toLowerCase()) {
            case "add" -> handleJobAdd(jobBo);
            case "update" -> handleJobUpdate(jobBo);
            case "delete" -> handleJobDelete(jobBo);
            case "changestatus" -> handleJobChangeStatus(jobBo);
            case "run" -> handleJobRun(jobBo);
            default -> AjaxResult.error("不支持的操作类型：" + operationType + "，请使用：add、update、delete、changeStatus、run");
        }, "定时任务操作");
    }

    private AjaxResult handleJobAdd(SysJobBo jobBo) {
        SysJob sysJob = new SysJob();
        BeanUtils.copyProperties(jobBo, sysJob);
        if (sysJob.getJobGroup() == null || sysJob.getJobGroup().isEmpty()) {
            sysJob.setJobGroup("DEFAULT");
        }
        if (sysJob.getStatus() == null || sysJob.getStatus().isEmpty()) {
            sysJob.setStatus("0");
        }
        R<Boolean> result = remoteJobService.addJob(sysJob);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("新增成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobUpdate(SysJobBo jobBo) {
        if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
            return formMissing("修改操作必须传入任务名称，请补充", jobBo, "jobName");
        }
        List<SysJob> jobs = getJobsByConditions(jobBo);
        if (jobs.isEmpty()) {
            return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
        }
        if (jobs.size() > 1) {
            return formMissing("查询到多个定时任务，请补充以下信息以精确定位：" + formatJobList(jobs), jobBo);
        }
        SysJob sysJob = new SysJob();
        BeanUtils.copyProperties(jobBo, sysJob);
        sysJob.setJobId(jobs.getFirst().getJobId());
        R<Boolean> result = remoteJobService.updateJob(sysJob);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobDelete(SysJobBo jobBo) {
        if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
            return formMissing("删除操作必须传入任务名称，请补充", jobBo, "jobName");
        }
        List<SysJob> jobs = getJobsByConditions(jobBo);
        if (jobs.isEmpty()) {
            return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
        }
        if (jobs.size() > 1) {
            return formMissing("查询到多个定时任务，请补充以下信息以精确定位：" + formatJobList(jobs), jobBo);
        }
        R<Boolean> result = remoteJobService.deleteJob(jobs.getFirst().getJobId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobChangeStatus(SysJobBo jobBo) {
        if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
            return formMissing("修改状态操作必须传入任务名称，请补充", jobBo, "jobName");
        }
        if (jobBo.getStatus() == null || jobBo.getStatus().isEmpty()) {
            return formMissing("修改状态操作必须传入任务状态：0正常 1暂停，请补充", jobBo, "status");
        }
        List<SysJob> jobs = getJobsByConditions(jobBo);
        if (jobs.isEmpty()) {
            return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
        }
        if (jobs.size() > 1) {
            return formMissing("查询到多个定时任务，请补充以下信息以精确定位：" + formatJobList(jobs), jobBo);
        }
        SysJob sysJob = new SysJob();
        sysJob.setJobId(jobs.getFirst().getJobId());
        sysJob.setStatus(jobBo.getStatus());
        R<Boolean> result = remoteJobService.changeJobStatus(sysJob);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("状态修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobRun(SysJobBo jobBo) {
        if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
            return formMissing("立即执行操作必须传入任务名称，请补充", jobBo, "jobName");
        }
        List<SysJob> jobs = getJobsByConditions(jobBo);
        if (jobs.isEmpty()) {
            return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
        }
        if (jobs.size() > 1) {
            return formMissing("查询到多个定时任务，请补充以下信息以精确定位：" + formatJobList(jobs), jobBo);
        }
        SysJob sysJob = new SysJob();
        sysJob.setJobId(jobs.getFirst().getJobId());
        R<Boolean> result = remoteJobService.runJob(sysJob);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("任务已触发执行") : AjaxResult.error(result.getMsg());
    }

    private List<SysJob> getJobsByConditions(SysJobBo jobBo) {
        SysJob query = new SysJob();
        if (jobBo.getJobName() != null && !jobBo.getJobName().isEmpty()) {
            query.setJobName(jobBo.getJobName());
        }
        if (jobBo.getJobGroup() != null && !jobBo.getJobGroup().isEmpty()) {
            query.setJobGroup(jobBo.getJobGroup());
        }
        R<List<SysJob>> result = remoteJobService.getJobList(query);
        if (result.getCode() == 200 && result.getData() != null) {
            return result.getData();
        }
        return List.of();
    }

    private String formatJobList(List<SysJob> jobs) {
        StringBuilder sb = new StringBuilder();
        for (SysJob j : jobs) {
            sb.append("\n- 任务名称：").append(j.getJobName())
              .append("，组名：").append(j.getJobGroup())
              .append("，Cron：").append(j.getCronExpression())
              .append("，状态：").append("0".equals(j.getStatus()) ? "正常" : "暂停");
        }
        return sb.toString();
    }
}
