package com.mall.system.api;

import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysMenuVo;
import com.mall.system.api.factory.RemoteMenuFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 系统服务
 *
 * @author mall
 */
@FeignClient(contextId = "remoteMenuService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteMenuFallbackFactory.class)
public interface RemoteMenuService {

    @PostMapping("/api/menu/getMenuByParam")
    public R<List<SysMenuVo>> getMenuByParam(@RequestBody SysMenuVo menu);
}
