package com.mall.system.api;

import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.factory.RemoteKbRagRetrieveFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 知识库RAG检索远程服务
 * <p>
 * 对标 Dify Knowledge Retrieve API，提供统一的向量检索接口。
 * 业务模块通过 kbType 参数区分不同知识库类型，chat 服务端统一执行检索逻辑。
 */
@FeignClient(contextId = "remoteKbRagRetrieveService", value = ServiceNameConstants.CHAT_SERVICE, fallbackFactory = RemoteKbRagRetrieveFallbackFactory.class)
public interface RemoteKbRagRetrieveService {

    /**
     * 按知识库类型检索相关文档片段
     *
     * @param question 用户自然语言问题
     * @param kbType   知识库类型（10-通用知识, 20-表结构专业知识, ...）
     * @return 检索到的文本片段
     */
    @GetMapping("/api/kb/retrieve")
    R<String> retrieve(@RequestParam("question") String question,
                        @RequestParam("kbType") String kbType);
}
