package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteNl2sqlBusinessRuleService;
import com.mall.system.api.domain.Nl2sqlBusinessRuleVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NL2SQL业务规则远程服务降级处理
 */
@Component
public class RemoteNl2sqlBusinessRuleFallbackFactory implements FallbackFactory<RemoteNl2sqlBusinessRuleService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteNl2sqlBusinessRuleFallbackFactory.class);

    @Override
    public RemoteNl2sqlBusinessRuleService create(Throwable throwable) {
        log.error("NL2SQL业务规则服务调用失败:{}", throwable.getMessage());
        return new RemoteNl2sqlBusinessRuleService() {
            @Override
            public R<List<Nl2sqlBusinessRuleVo>> list() {
                return R.fail("NL2SQL业务规则服务调用失败:" + throwable.getMessage());
            }
        };
    }
}