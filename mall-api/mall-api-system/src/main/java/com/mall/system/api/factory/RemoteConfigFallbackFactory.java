package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteConfigService;
import com.mall.system.api.domain.SysConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 参数配置服务降级处理
 * 
 * @author mall
 */
@Component
public class RemoteConfigFallbackFactory implements FallbackFactory<RemoteConfigService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteConfigFallbackFactory.class);

    @Override
    public RemoteConfigService create(Throwable throwable)
    {
        log.error("参数配置服务调用失败:{}", throwable.getMessage());
        return new RemoteConfigService()
        {
            @Override
            public R<List<SysConfig>> getConfigList(SysConfig config)
            {
                return R.fail("查询参数配置失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addConfig(SysConfig config)
            {
                return R.fail("新增参数配置失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updateConfig(SysConfig config)
            {
                return R.fail("修改参数配置失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deleteConfig(Long configId)
            {
                return R.fail("删除参数配置失败:" + throwable.getMessage());
            }
        };
    }
}