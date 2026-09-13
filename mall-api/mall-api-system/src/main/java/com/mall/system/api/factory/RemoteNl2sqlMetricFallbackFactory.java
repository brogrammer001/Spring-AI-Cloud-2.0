package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteNl2sqlMetricService;
import com.mall.system.api.domain.Nl2sqlMetricVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NL2SQL指标定义远程服务降级处理
 */
@Component
public class RemoteNl2sqlMetricFallbackFactory implements FallbackFactory<RemoteNl2sqlMetricService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteNl2sqlMetricFallbackFactory.class);

    @Override
    public RemoteNl2sqlMetricService create(Throwable throwable) {
        log.error("NL2SQL指标定义服务调用失败:{}", throwable.getMessage());
        return new RemoteNl2sqlMetricService() {
            @Override
            public R<List<Nl2sqlMetricVo>> list() {
                return R.fail("NL2SQL指标定义服务调用失败:" + throwable.getMessage());
            }
        };
    }
}