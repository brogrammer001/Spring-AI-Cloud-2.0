package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteJobService;
import com.mall.system.api.domain.SysJob;
import com.mall.system.api.domain.SysJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务服务降级处理
 * 
 * @author mall
 */
@Component
public class RemoteJobFallbackFactory implements FallbackFactory<RemoteJobService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteJobFallbackFactory.class);

    @Override
    public RemoteJobService create(Throwable throwable)
    {
        log.error("定时任务服务调用失败:{}", throwable.getMessage());
        return new RemoteJobService()
        {
            @Override
            public R<List<SysJob>> getJobList(SysJob job)
            {
                return R.fail("查询定时任务失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addJob(SysJob job)
            {
                return R.fail("新增定时任务失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updateJob(SysJob job)
            {
                return R.fail("修改定时任务失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deleteJob(Long jobId)
            {
                return R.fail("删除定时任务失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> changeJobStatus(SysJob job)
            {
                return R.fail("修改定时任务状态失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> runJob(SysJob job)
            {
                return R.fail("执行定时任务失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysJobLog>> getJobLogList(SysJobLog jobLog)
            {
                return R.fail("查询定时任务日志失败:" + throwable.getMessage());
            }
        };
    }
}