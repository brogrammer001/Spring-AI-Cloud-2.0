package com.mall.system.api;

import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.Nl2sqlBusinessRuleVo;
import com.mall.system.api.factory.RemoteNl2sqlBusinessRuleFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * NL2SQL业务规则远程服务（Feign）
 * <p>
 * 指向 mall-ai-chat 服务，通过 @InnerAuth 内部 API 获取 NL2SQL 业务规则语义定义，
 * 替代 mcp-server 中对 mall_ai 库执行 SELECT * FROM nl2sql_business_rule 的裸 SQL 调用。
 */
@FeignClient(contextId = "remoteNl2sqlBusinessRuleService", value = ServiceNameConstants.CHAT_SERVICE,
    fallbackFactory = RemoteNl2sqlBusinessRuleFallbackFactory.class)
public interface RemoteNl2sqlBusinessRuleService {

    /**
     * 查询启用的NL2SQL业务规则列表
     *
     * @return 业务规则列表
     */
    @GetMapping("/api/nl2sql/business-rule/list")
    R<List<Nl2sqlBusinessRuleVo>> list();
}