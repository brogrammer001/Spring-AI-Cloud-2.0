package com.mall.system.api;

import java.util.List;
import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysJob;
import com.mall.system.api.domain.SysJobLog;
import com.mall.system.api.factory.RemoteJobFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务服务
 * 
 * @author mall
 */
@FeignClient(contextId = "remoteJobService", value = ServiceNameConstants.JOB_SERVICE, fallbackFactory = RemoteJobFallbackFactory.class)
public interface RemoteJobService {

    @PostMapping("/api/job/list")
    public R<List<SysJob>> getJobList(@RequestBody SysJob job);

    @PostMapping("/api/job/add")
    public R<Boolean> addJob(@RequestBody SysJob job);

    @PutMapping("/api/job/update")
    public R<Boolean> updateJob(@RequestBody SysJob job);

    @DeleteMapping("/api/job/{jobId}")
    public R<Boolean> deleteJob(@PathVariable("jobId") Long jobId);

    @PutMapping("/api/job/changeStatus")
    public R<Boolean> changeJobStatus(@RequestBody SysJob job);

    @PutMapping("/api/job/run")
    public R<Boolean> runJob(@RequestBody SysJob job);

    @PostMapping("/api/job/log/list")
    public R<List<SysJobLog>> getJobLogList(@RequestBody SysJobLog jobLog);

}