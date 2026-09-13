package com.mall.aichat.api;

import com.mall.aichat.domain.Nl2sqlDimension;
import com.mall.aichat.service.INl2sqlDimensionService;
import com.mall.common.core.domain.R;
import com.mall.common.security.annotation.InnerAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NL2SQL维度定义内部API（供远程服务Feign调用）
 * <p>
 * 供 mall-ai-mcp-server 等远程服务通过 Feign 调用，获取语义层维度定义。
 * 替代调用方直接对 mall_ai 库执行 SELECT * FROM nl2sql_dimension 的裸 SQL 查询。
 */
@RestController
@RequestMapping("/api/nl2sql/dimension")
public class Nl2sqlDimensionApi {

    @Autowired
    private INl2sqlDimensionService nl2sqlDimensionService;

    /**
     * 查询启用的NL2SQL维度定义列表
     *
     * @return 维度定义列表
     */
    @GetMapping("/list")
    @InnerAuth
    public R<List<Nl2sqlDimension>> list() {
        Nl2sqlDimension query = new Nl2sqlDimension();
        query.setEnabled("1");
        return R.ok(nl2sqlDimensionService.selectNl2sqlDimensionList(query));
    }
}