package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sql")
public class SysSqlApi extends BaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/tables")
    @InnerAuth
    public R<List<String>> getAllTableNames() {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()";
        return R.ok(jdbcTemplate.queryForList(sql, String.class));
    }

    @GetMapping("/columns/{tableName}")
    @InnerAuth
    public R<List<Map<String, Object>>> getTableColumns(@PathVariable("tableName") String tableName) {
        String sql = "SELECT column_name AS COLUMN_NAME, data_type AS DATA_TYPE, column_comment AS COLUMN_COMMENT " +
                "FROM information_schema.columns WHERE table_name = ? AND table_schema = DATABASE()";
        return R.ok(jdbcTemplate.queryForList(sql, tableName));
    }

    @PostMapping("/execute")
    @InnerAuth
    public R<List<Map<String, Object>>> executeSelect(@RequestBody String sql) {
        return R.ok(jdbcTemplate.queryForList(sql));
    }
}
