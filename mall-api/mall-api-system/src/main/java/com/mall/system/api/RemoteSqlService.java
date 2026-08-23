package com.mall.system.api;

import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SqlQueryRequest;
import com.mall.system.api.factory.RemoteSqlFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * SQL查询服务
 */
@FeignClient(contextId = "remoteSqlService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteSqlFallbackFactory.class)
public interface RemoteSqlService {

    /**
     * 执行SELECT查询
     * <p>
     * 使用DTO封装SQL，避免Feign直接传输String时
     * StringHttpMessageConverter 默认 ISO-8859-1 编码导致中文被转义为 '?'
     */
    @PostMapping("/api/sql/execute")
    R<List<Map<String, Object>>> executeSelect(@RequestBody SqlQueryRequest request);
}
