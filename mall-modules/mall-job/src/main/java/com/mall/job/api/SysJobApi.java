package com.mall.job.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.job.domain.SysJob;
import com.mall.job.domain.SysJobLog;
import com.mall.job.service.ISysJobLogService;
import com.mall.job.service.ISysJobService;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job")
public class SysJobApi extends BaseController {

    @Autowired
    private ISysJobService jobService;

    @Autowired
    private ISysJobLogService jobLogService;

    @PostMapping("/list")
    @InnerAuth
    public R<List<SysJob>> getJobList(@RequestBody SysJob job) {
        List<SysJob> list = jobService.selectJobList(job);
        return R.ok(list);
    }

    @PostMapping("/add")
    @InnerAuth
    public R<Boolean> addJob(@RequestBody SysJob job) {
        try {
            return R.ok(jobService.insertJob(job) > 0);
        } catch (Exception e) {
            return R.fail("新增定时任务失败:" + e.getMessage());
        }
    }

    @PutMapping("/update")
    @InnerAuth
    public R<Boolean> updateJob(@RequestBody SysJob job) {
        try {
            return R.ok(jobService.updateJob(job) > 0);
        } catch (Exception e) {
            return R.fail("修改定时任务失败:" + e.getMessage());
        }
    }

    @DeleteMapping("/{jobId}")
    @InnerAuth
    public R<Boolean> deleteJob(@PathVariable("jobId") Long jobId) {
        try {
            SysJob job = new SysJob();
            job.setJobId(jobId);
            return R.ok(jobService.deleteJob(job) > 0);
        } catch (Exception e) {
            return R.fail("删除定时任务失败:" + e.getMessage());
        }
    }

    @PutMapping("/changeStatus")
    @InnerAuth
    public R<Boolean> changeJobStatus(@RequestBody SysJob job) {
        try {
            return R.ok(jobService.changeStatus(job) > 0);
        } catch (Exception e) {
            return R.fail("修改定时任务状态失败:" + e.getMessage());
        }
    }

    @PutMapping("/run")
    @InnerAuth
    public R<Boolean> runJob(@RequestBody SysJob job) {
        try {
            return R.ok(jobService.run(job));
        } catch (Exception e) {
            return R.fail("执行定时任务失败:" + e.getMessage());
        }
    }

    @PostMapping("/log/list")
    @InnerAuth
    public R<List<SysJobLog>> getJobLogList(@RequestBody SysJobLog jobLog) {
        List<SysJobLog> list = jobLogService.selectJobLogList(jobLog);
        return R.ok(list);
    }

}