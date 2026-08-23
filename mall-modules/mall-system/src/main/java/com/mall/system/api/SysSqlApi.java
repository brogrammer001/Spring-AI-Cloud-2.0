package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SqlQueryRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sql")
public class SysSqlApi extends BaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/execute")
    @InnerAuth
    public R<List<Map<String, Object>>> executeSelect(@RequestBody SqlQueryRequest request) {
        List<Map<String, @Nullable Object>> data = jdbcTemplate.queryForList(request.getSql());
        return R.ok(data);
    }
}
