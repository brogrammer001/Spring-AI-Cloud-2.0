package com.mall.chatmcp.sevice.impl;

import com.mall.chatmcp.bo.SysJobBo;
import com.mall.common.core.domain.R;
import com.mall.common.core.web.domain.AjaxResult;
import com.mall.system.api.RemoteJobService;
import com.mall.system.api.domain.SysJob;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobToolServiceImpl extends BaseToolServiceImpl {

    @Autowired
    private RemoteJobService remoteJobService;

    @Autowired
    public void setValidator(Validator validator) {
        super.setValidator(validator);
    }

    @Tool(description = "定时任务的新增、修改、删除、状态修改、立即执行。参数包含 operationType(add/update/delete/changeStatus/run)和任务实体。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
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
                if (!hasRecordId(jobBo) && (jobBo.getJobName() == null || jobBo.getJobName().isEmpty())) {
                    return formMissing(opLabel(operationType) + "操作必须传入任务名称，请补充", jobBo, "jobName");
                }
            }
            case "changestatus" -> {
                if (!hasRecordId(jobBo) && (jobBo.getJobName() == null || jobBo.getJobName().isEmpty())) {
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
        Long jobId = jobBo.getJobId();
        if (jobId == null) {
            if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
                return formMissing("修改操作必须传入任务名称，请补充", jobBo, "jobName");
            }
            List<SysJob> jobs = getJobsByConditions(jobBo);
            if (jobs.isEmpty()) {
                return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
            }
            if (jobs.size() > 1) {
                return formSelect("查询到 " + jobs.size() + " 个定时任务，请选择要修改的任务：",
                    jobBo, jobs, SysJob::getJobId, this::jobLabel, "jobId", "定时任务");
            }
            jobId = jobs.getFirst().getJobId();
        }
        SysJob sysJob = new SysJob();
        BeanUtils.copyProperties(jobBo, sysJob);
        sysJob.setJobId(jobId);
        R<Boolean> result = remoteJobService.updateJob(sysJob);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobDelete(SysJobBo jobBo) {
        Long jobId = jobBo.getJobId();
        if (jobId == null) {
            if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
                return formMissing("删除操作必须传入任务名称，请补充", jobBo, "jobName");
            }
            List<SysJob> jobs = getJobsByConditions(jobBo);
            if (jobs.isEmpty()) {
                return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
            }
            if (jobs.size() > 1) {
                return formSelect("查询到 " + jobs.size() + " 个定时任务，请选择要删除的任务：",
                    jobBo, jobs, SysJob::getJobId, this::jobLabel, "jobId", "定时任务");
            }
            jobId = jobs.getFirst().getJobId();
        }
        R<Boolean> result = remoteJobService.deleteJob(jobId);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("删除成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobChangeStatus(SysJobBo jobBo) {
        if (jobBo.getStatus() == null || jobBo.getStatus().isEmpty()) {
            return formMissing("修改状态操作必须传入任务状态：0正常 1暂停，请补充", jobBo, "status");
        }
        Long jobId = jobBo.getJobId();
        if (jobId == null) {
            if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
                return formMissing("修改状态操作必须传入任务名称，请补充", jobBo, "jobName");
            }
            List<SysJob> jobs = getJobsByConditions(jobBo);
            if (jobs.isEmpty()) {
                return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
            }
            if (jobs.size() > 1) {
                return formSelect("查询到 " + jobs.size() + " 个定时任务，请选择要修改状态的任务：",
                    jobBo, jobs, SysJob::getJobId, this::jobLabel, "jobId", "定时任务");
            }
            jobId = jobs.getFirst().getJobId();
        }
        SysJob sysJob = new SysJob();
        sysJob.setJobId(jobId);
        sysJob.setStatus(jobBo.getStatus());
        R<Boolean> result = remoteJobService.changeJobStatus(sysJob);
        return result.getCode() == 200 && result.getData() ? AjaxResult.success("状态修改成功") : AjaxResult.error(result.getMsg());
    }

    private AjaxResult handleJobRun(SysJobBo jobBo) {
        Long jobId = jobBo.getJobId();
        if (jobId == null) {
            if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
                return formMissing("立即执行操作必须传入任务名称，请补充", jobBo, "jobName");
            }
            List<SysJob> jobs = getJobsByConditions(jobBo);
            if (jobs.isEmpty()) {
                return AjaxResult.error("定时任务不存在：" + jobBo.getJobName());
            }
            if (jobs.size() > 1) {
                return formSelect("查询到 " + jobs.size() + " 个定时任务，请选择要立即执行的任务：",
                    jobBo, jobs, SysJob::getJobId, this::jobLabel, "jobId", "定时任务");
            }
            jobId = jobs.getFirst().getJobId();
        }
        SysJob sysJob = new SysJob();
        sysJob.setJobId(jobId);
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

    private String jobLabel(SysJob j) {
        return "任务名称：" + j.getJobName()
            + "，组名：" + j.getJobGroup()
            + "，Cron：" + j.getCronExpression()
            + "，状态：" + ("0".equals(j.getStatus()) ? "正常" : "暂停");
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

    @Tool(description = "批量新增定时任务。参数：定时任务列表(jobList)，每个元素需包含任务名称、调用目标字符串、Cron表达式等必填信息，可填写任务组名、策略、备注。未填组名默认DEFAULT、状态默认正常。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchAddJobs(
            @ToolParam(description = "定时任务信息列表") List<SysJobBo> jobList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(jobList, "定时任务");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            for (SysJobBo jobBo : jobList) {
                AjaxResult validateResult = validate(jobBo);
                if (validateResult != null) {
                    return validateResult;
                }
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("jobList", jobList.stream().map(this::toMap).toList());
                return confirmResult("batchAddJobs", "add", "确认批量新增定时任务",
                    "即将批量新增 " + jobList.size() + " 个定时任务，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功新增：", "以下定时任务新增失败：");
            for (SysJobBo jobBo : jobList) {
                SysJob sysJob = new SysJob();
                BeanUtils.copyProperties(jobBo, sysJob);
                if (sysJob.getJobGroup() == null || sysJob.getJobGroup().isEmpty()) {
                    sysJob.setJobGroup("DEFAULT");
                }
                if (sysJob.getStatus() == null || sysJob.getStatus().isEmpty()) {
                    sysJob.setStatus("0");
                }
                R<Boolean> result = remoteJobService.addJob(sysJob);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(jobBo.getJobName());
                } else {
                    batch.addFail(jobBo.getJobName(), result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量新增定时任务");
    }

    @Tool(description = "批量删除定时任务。参数：任务名称列表(jobNames)。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchDeleteJobs(
            @ToolParam(description = "任务名称列表") List<String> jobNames,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(jobNames, "定时任务");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("jobNames", jobNames);
                return confirmResult("batchDeleteJobs", "delete", "确认批量删除定时任务",
                    "即将批量删除 " + jobNames.size() + " 个定时任务：" + joinNames(jobNames) + "，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功删除：", "以下定时任务删除失败：");
            for (String jobName : jobNames) {
                SysJobBo query = new SysJobBo();
                query.setJobName(jobName);
                List<SysJob> jobs = getJobsByConditions(query);
                if (jobs.isEmpty()) {
                    batch.addFail(jobName, "定时任务不存在");
                    continue;
                }
                if (jobs.size() > 1) {
                    batch.addFail(jobName, "匹配到多条记录，请使用更精确的任务名称或携带任务组名");
                    continue;
                }
                R<Boolean> result = remoteJobService.deleteJob(jobs.getFirst().getJobId());
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(jobName);
                } else {
                    batch.addFail(jobName, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量删除定时任务");
    }

    @Tool(description = "批量修改定时任务。参数：定时任务列表(jobList)，每个元素需包含任务名称(jobName)或任务ID(jobId)用于定位，另可填写任务组名、调用目标、Cron表达式、状态、备注等要修改的字段。" + CONFIRMED_MSG + " [JSON]", returnDirect = true)
    public AjaxResult batchUpdateJobs(
            @ToolParam(description = "定时任务信息列表") List<SysJobBo> jobList,
            @ToolParam(description = "用户确认标志，用户在前端点击同意按钮后为true，AI不要自行设置该值") Boolean confirmed) {
        return executeWithErrorHandling(() -> {
            AjaxResult sizeCheck = checkBatchSize(jobList, "定时任务");
            if (sizeCheck != null) {
                return sizeCheck;
            }
            if (!Boolean.TRUE.equals(confirmed)) {
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("jobList", jobList.stream().map(this::toMap).toList());
                return confirmResult("batchUpdateJobs", "update", "确认批量修改定时任务",
                    "即将批量修改 " + jobList.size() + " 个定时任务，请确认是否继续。", args);
            }
            BatchResult batch = new BatchResult("成功修改：", "以下定时任务修改失败：");
            for (SysJobBo jobBo : jobList) {
                String label = jobBo.getJobName() != null && !jobBo.getJobName().isEmpty()
                    ? jobBo.getJobName() : "ID=" + jobBo.getJobId();
                Long jobId = jobBo.getJobId();
                if (jobId == null) {
                    if (jobBo.getJobName() == null || jobBo.getJobName().isEmpty()) {
                        batch.addFail(label, "缺少任务名称或任务ID");
                        continue;
                    }
                    List<SysJob> jobs = getJobsByConditions(jobBo);
                    if (jobs.isEmpty()) {
                        batch.addFail(label, "定时任务不存在");
                        continue;
                    }
                    if (jobs.size() > 1) {
                        batch.addFail(label, "匹配到多条记录，请携带任务ID");
                        continue;
                    }
                    jobId = jobs.getFirst().getJobId();
                }

                SysJob sysJob = new SysJob();
                BeanUtils.copyProperties(jobBo, sysJob);
                sysJob.setJobId(jobId);
                R<Boolean> result = remoteJobService.updateJob(sysJob);
                if (result.getCode() == 200 && result.getData()) {
                    batch.addSuccess(label);
                } else {
                    batch.addFail(label, result.getMsg());
                }
            }
            return batch.toResult();
        }, "批量修改定时任务");
    }
}
