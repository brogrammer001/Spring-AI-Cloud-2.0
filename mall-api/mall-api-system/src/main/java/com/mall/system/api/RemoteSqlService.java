package com.mall.system.api;

import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.factory.RemoteSqlFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SQL查询服务
 */
@FeignClient(contextId = "remoteSqlService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteSqlFallbackFactory.class)
public interface RemoteSqlService {

    /**
     * 获取数据库所有表名
     */
    @GetMapping("/api/sql/tables")
    R<List<String>> getAllTableNames();

    /**
     * 获取指定表的列信息
     */
    @GetMapping("/api/sql/columns/{tableName}")
    R<List<Map<String, Object>>> getTableColumns(@PathVariable("tableName") String tableName);

    /**
     * 执行SELECT查询
     */
    @PostMapping("/api/sql/execute")
    R<List<Map<String, Object>>> executeSelect(@RequestBody String sql);
}
