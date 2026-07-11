package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteMenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 文件服务降级处理
 * 
 * @author mall
 */
@Component
public class RemoteMenuFallbackFactory implements FallbackFactory<RemoteMenuService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteMenuFallbackFactory.class);

    @Override
    public RemoteMenuService create(Throwable throwable)
    {
        log.error("菜单服务调用失败:{}", throwable.getMessage());
        return menu -> R.fail("查询菜单失败:" + throwable.getMessage());
    }
}
