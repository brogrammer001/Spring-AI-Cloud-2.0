package com.mall.system.api;

import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.Nl2sqlMetricVo;
import com.mall.system.api.factory.RemoteNl2sqlMetricFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * NL2SQL指标定义远程服务（Feign）
 * <p>
 * 指向 mall-ai-chat 服务，通过 {@code @InnerAuth} 内部 API 获取语义层指标定义，
 * 替代 mcp-server 中直接对 mall_ai 库执行 {@code SELECT * FROM nl2sql_metric} 的裸 SQL 调用。
 */
@FeignClient(contextId = "remoteNl2sqlMetricService", value = ServiceNameConstants.CHAT_SERVICE,
    fallbackFactory = RemoteNl2sqlMetricFallbackFactory.class)
public interface RemoteNl2sqlMetricService {

    /**
     * 查询启用的NL2SQL指标定义列表
     *
     * @return 指标定义列表
     */
    @GetMapping("/api/nl2sql/metric/list")
    R<List<Nl2sqlMetricVo>> list();
}