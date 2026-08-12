package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteKbRagRetrieveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 知识库RAG检索远程服务降级处理
 */
@Component
public class RemoteKbRagRetrieveFallbackFactory implements FallbackFactory<RemoteKbRagRetrieveService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteKbRagRetrieveFallbackFactory.class);

    @Override
    public RemoteKbRagRetrieveService create(Throwable throwable) {
        log.error("知识库RAG检索服务调用失败:{}", throwable.getMessage());
        return new RemoteKbRagRetrieveService() {
            @Override
            public R<String> retrieve(String question, String kbType) {
                return R.fail("知识库RAG检索失败:" + throwable.getMessage());
            }
        };
    }
}
