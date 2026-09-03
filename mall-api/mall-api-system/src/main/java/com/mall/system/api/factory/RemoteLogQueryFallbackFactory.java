package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteLogQueryService;
import com.mall.system.api.domain.SysLogininfor;
import com.mall.system.api.domain.SysOperLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 日志查询服务降级处理
 * 
 * @author mall
 */
@Component
public class RemoteLogQueryFallbackFactory implements FallbackFactory<RemoteLogQueryService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteLogQueryFallbackFactory.class);

    @Override
    public RemoteLogQueryService create(Throwable throwable)
    {
        log.error("日志查询服务调用失败:{}", throwable.getMessage());
        return new RemoteLogQueryService()
        {
            @Override
            public R<List<SysOperLog>> getOperLogList(SysOperLog operLog)
            {
                return R.fail("查询操作日志失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysLogininfor>> getLogininforList(SysLogininfor logininfor)
            {
                return R.fail("查询登录日志失败:" + throwable.getMessage());
            }
        };
    }
}