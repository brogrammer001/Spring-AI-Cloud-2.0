package com.mall.system.api;

import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.Nl2sqlEvalVo;
import com.mall.system.api.factory.RemoteNl2sqlEvalFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * NL2SQL黄金评测集远程服务（Feign）
 * <p>
 * 指向 mall-ai-chat 服务，通过 @InnerAuth 内部 API 获取 NL2SQL 黄金评测集，
 * 替代 mcp-server 中对 mall_ai 库执行 SELECT * FROM nl2sql_eval 的裸 SQL 调用。
 */
@FeignClient(contextId = "remoteNl2sqlEvalService", value = ServiceNameConstants.CHAT_SERVICE,
    fallbackFactory = RemoteNl2sqlEvalFallbackFactory.class)
public interface RemoteNl2sqlEvalService {

    /**
     * 查询启用的NL2SQL黄金评测集列表
     *
     * @return 评测集列表
     */
    @GetMapping("/api/nl2sql/eval/list")
    R<List<Nl2sqlEvalVo>> list();
}