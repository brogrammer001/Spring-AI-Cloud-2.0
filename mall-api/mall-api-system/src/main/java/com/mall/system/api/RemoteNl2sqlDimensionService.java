package com.mall.system.api;

import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.Nl2sqlDimensionVo;
import com.mall.system.api.factory.RemoteNl2sqlDimensionFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * NL2SQL维度定义远程服务（Feign）
 * <p>
 * 指向 mall-ai-chat 服务，通过 @InnerAuth 内部 API 获取 NL2SQL 维度语义定义，
 * 替代 mcp-server 中对 mall_ai 库执行 SELECT * FROM nl2sql_dimension 的裸 SQL 调用。
 */
@FeignClient(contextId = "remoteNl2sqlDimensionService", value = ServiceNameConstants.CHAT_SERVICE,
    fallbackFactory = RemoteNl2sqlDimensionFallbackFactory.class)
public interface RemoteNl2sqlDimensionService {

    /**
     * 查询启用的NL2SQL维度定义列表
     *
     * @return 维度定义列表
     */
    @GetMapping("/api/nl2sql/dimension/list")
    R<List<Nl2sqlDimensionVo>> list();
}