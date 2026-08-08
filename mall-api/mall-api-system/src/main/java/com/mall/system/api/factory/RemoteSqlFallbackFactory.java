package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteSqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SQL查询服务降级处理
 */
@Component
public class RemoteSqlFallbackFactory implements FallbackFactory<RemoteSqlService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteSqlFallbackFactory.class);

    @Override
    public RemoteSqlService create(Throwable throwable) {
        log.error("SQL查询服务调用失败:{}", throwable.getMessage());
        return new RemoteSqlService() {
            @Override
            public R<List<String>> getAllTableNames() {
                return R.fail("获取表名列表失败:" + throwable.getMessage());
            }

            @Override
            public R<List<Map<String, Object>>> getTableColumns(String tableName) {
                return R.fail("获取表结构失败:" + throwable.getMessage());
            }

            @Override
            public R<List<Map<String, Object>>> executeSelect(String sql) {
                return R.fail("执行SQL查询失败:" + throwable.getMessage());
            }
        };
    }
}
