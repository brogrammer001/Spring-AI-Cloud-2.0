package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteNl2sqlEvalService;
import com.mall.system.api.domain.Nl2sqlEvalVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NL2SQL黄金评测集远程服务降级处理
 */
@Component
public class RemoteNl2sqlEvalFallbackFactory implements FallbackFactory<RemoteNl2sqlEvalService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteNl2sqlEvalFallbackFactory.class);

    @Override
    public RemoteNl2sqlEvalService create(Throwable throwable) {
        log.error("NL2SQL黄金评测集服务调用失败:{}", throwable.getMessage());
        return new RemoteNl2sqlEvalService() {
            @Override
            public R<List<Nl2sqlEvalVo>> list() {
                return R.fail("NL2SQL黄金评测集服务调用失败:" + throwable.getMessage());
            }
        };
    }
}