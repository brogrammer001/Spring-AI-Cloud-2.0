package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteNl2sqlDimensionService;
import com.mall.system.api.domain.Nl2sqlDimensionVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NL2SQL维度定义远程服务降级处理
 */
@Component
public class RemoteNl2sqlDimensionFallbackFactory implements FallbackFactory<RemoteNl2sqlDimensionService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteNl2sqlDimensionFallbackFactory.class);

    @Override
    public RemoteNl2sqlDimensionService create(Throwable throwable) {
        log.error("NL2SQL维度定义服务调用失败:{}", throwable.getMessage());
        return new RemoteNl2sqlDimensionService() {
            @Override
            public R<List<Nl2sqlDimensionVo>> list() {
                return R.fail("NL2SQL维度定义服务调用失败:" + throwable.getMessage());
            }
        };
    }
}