package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysJobBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteJobService;
import com.mall.system.api.domain.SysJob;
import com.mall.system.api.domain.SysJobLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    @Tool(description = "定时任务的新增、修改、删除、状态修改、立即执行。参数包含 operationType(add/update/delete/changeStatus/run)和任务实体。")
    public AjaxResult jobCrud(SysJobBo jobBo) {
        String operationType = jobBo.getOperationType();
        if (operationType == null || operationType.isEmpty()) {
            return AjaxResult.error("操作类型不能为空，请指定：add、update、delete、changeStatus、run");
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

    @Tool(description = "查询定时任务列表。参数：任务名称(jobName)或任务组名(jobGroup)，可选。")
    public AjaxResult jobQuery(
            @ToolParam(description = "任务名称，可选") String jobName,
            @ToolParam(description = "任务组名，可选") String jobGroup) {
        return executeWithErrorHandling(() -> {
            SysJob query = new SysJob();
            if (jobName != null && !jobName.isEmpty()) {
                query.setJobName(jobName);
            }
            if (jobGroup != null && !jobGroup.isEmpty()) {
                query.setJobGroup(jobGroup);
            }
            R<List<SysJob>> result = remoteJobService.getJobList(query);
            if (result.getCode() == 200 && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    return AjaxResult.error("未查询到相关定时任务");
                }
                StringBuilder sb = new StringBuilder("查询到 " + result.getData().size() + " 个定时任务：\n");
                for (SysJob j : result.getData()) {
                    sb.append("- 任务名称：").append(j.getJobName())
                      .append("，组名：").append(j.getJobGroup())
                      .append("，Cron：").append(j.getCronExpression())
                      .append("，状态：").append("0".equals(j.getStatus()) ? "正常" : "暂停")
                      .append("\n");
                }
                return AjaxResult.success(sb.toString());
            }
            return AjaxResult.error(result.getMsg());
        }, "查询定时任务");
    }

    @Tool(description = "查询定时任务执行日志。参数：任务名称(jobName)或执行状态(status: 0正常 1失败)，可选。")
    public AjaxResult jobLogQuery(
            @ToolParam(description = "任务名称，可选") String jobName,
            @ToolParam(description = "执行状态：0正常 1失败，可选") String status) {
        return executeWithErrorHandling(() -> {
            SysJobLog query = new SysJobLog();
            if (jobName != null && !jobName.isEmpty()) {
                query.setJobName(jobName);
            }
            if (status != null && !status.isEmpty()) {
                query.setStatus(status);
            }
            R<List<SysJobLog>> result = remoteJobService.getJobLogList(query);
            if (result.getCode() == 200 && result.getData() != null) {
                if (result.getData().isEmpty()) {
                    return AjaxResult.error("未查询到相关任务日志");
                }
                List<SysJobLog> logs = result.getData();
                StringBuilder sb = new StringBuilder("查询到 " + logs.size() + " 条任务日志：\n");
                int limit = Math.min(10, logs.size());
                for (int i = 0; i < limit; i++) {
                    SysJobLog log = logs.get(i);
                    sb.append(i + 1).append(". 任务：").append(log.getJobName())
                      .append("，组名：").append(log.getJobGroup())
                      .append("，状态：").append("0".equals(log.getStatus()) ? "正常" : "失败")
                      .append("，信息：").append(log.getJobMessage() != null ? log.getJobMessage() : "无")
                      .append("\n");
                }
                if (logs.size() > limit) {
                    sb.append("... 共 ").append(logs.size()).append(" 条");
                }
                return AjaxResult.success(sb.toString());
            }
            return AjaxResult.error(result.getMsg());
        }, "查询任务日志");
    }

    private AjaxResult handleJobAdd(SysJobBo jobBo) {
        AjaxResult validateResult = validate(jobBo, "sysJobBo");
        if (validateResult != null) {
            return validateResult;
        }
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
            return AjaxResult.error("修改操作必须传入任务名称");
        }
        List<SysJob> jobs = getJobsByConditions(jobBo);
        if (jobs.isEmpty()) {
            return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
        }
        if (jobs.size() > 1) {
            return AjaxResult.error("查询到多个定时任务，请补充更多信息后重试：" + formatJobList(jobs));
        }
        SysJob sysJob = new SysJob();
        BeanUtils.copyProperties(jobBo, sysJob);
        sysJob.setJobId(jobs.getFirst().getJobId());
        R<Boolean> result = remoteJobService.updateJob(sysJob);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobDelete(SysJobBo jobBo) {
        if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
            return AjaxResult.error("删除操作必须传入任务名称");
        }
        List<SysJob> jobs = getJobsByConditions(jobBo);
        if (jobs.isEmpty()) {
            return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
        }
        if (jobs.size() > 1) {
            return AjaxResult.error("查询到多个定时任务，请补充更多信息后重试：" + formatJobList(jobs));
        }
        R<Boolean> result = remoteJobService.deleteJob(jobs.getFirst().getJobId());
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobChangeStatus(SysJobBo jobBo) {
        if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
            return AjaxResult.error("修改状态操作必须传入任务名称");
        }
        if (jobBo.getStatus() == null || jobBo.getStatus().isEmpty()) {
            return AjaxResult.error("修改状态操作必须传入任务状态：0正常 1暂停");
        }
        List<SysJob> jobs = getJobsByConditions(jobBo);
        if (jobs.isEmpty()) {
            return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
        }
        if (jobs.size() > 1) {
            return AjaxResult.error("查询到多个定时任务，请补充更多信息后重试：" + formatJobList(jobs));
        }
        SysJob sysJob = new SysJob();
        sysJob.setJobId(jobs.getFirst().getJobId());
        sysJob.setStatus(jobBo.getStatus());
        R<Boolean> result = remoteJobService.changeJobStatus(sysJob);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("状态修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobRun(SysJobBo jobBo) {
        if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
            return AjaxResult.error("立即执行操作必须传入任务名称");
        }
        List<SysJob> jobs = getJobsByConditions(jobBo);
        if (jobs.isEmpty()) {
            return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
        }
        if (jobs.size() > 1) {
            return AjaxResult.error("查询到多个定时任务，请补充更多信息后重试：" + formatJobList(jobs));
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