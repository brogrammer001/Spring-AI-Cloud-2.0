package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteSqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * SQL查询服务降级处理
 */
@Component
public class RemoteSqlFallbackFactory implements FallbackFactory<RemoteSqlService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteSqlFallbackFactory.class);

    @Override
    public RemoteSqlService create(Throwable throwable) {
        log.error("SQL查询服务调用失败:{}", throwable.getMessage());
        return request -> R.fail("执行SQL查询失败:" + throwable.getMessage());
    }
}