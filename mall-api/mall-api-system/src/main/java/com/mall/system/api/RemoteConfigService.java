package com.mall.system.api;

import java.util.List;
import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysConfig;
import com.mall.system.api.factory.RemoteConfigFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 参数配置服务
 * 
 * @author mall
 */
@FeignClient(contextId = "remoteConfigService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteConfigFallbackFactory.class)
public interface RemoteConfigService {

    @PostMapping("/api/config/list")
    public R<List<SysConfig>> getConfigList(@RequestBody SysConfig config);

    @PostMapping("/api/config/add")
    public R<Boolean> addConfig(@RequestBody SysConfig config);

    @PutMapping("/api/config/update")
    public R<Boolean> updateConfig(@RequestBody SysConfig config);

    @DeleteMapping("/api/config/{configId}")
    public R<Boolean> deleteConfig(@PathVariable("configId") Long configId);

}