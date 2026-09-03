package com.mall.system.api;

import java.util.List;
import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysLogininfor;
import com.mall.system.api.domain.SysOperLog;
import com.mall.system.api.factory.RemoteLogQueryFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 日志查询服务
 * 
 * @author mall
 */
@FeignClient(contextId = "remoteLogQueryService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteLogQueryFallbackFactory.class)
public interface RemoteLogQueryService {

    @PostMapping("/api/operlog/list")
    public R<List<SysOperLog>> getOperLogList(@RequestBody SysOperLog operLog);

    @PostMapping("/api/logininfor/list")
    public R<List<SysLogininfor>> getLogininforList(@RequestBody SysLogininfor logininfor);

}