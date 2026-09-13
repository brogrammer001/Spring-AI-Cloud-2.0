package com.mall.aichat.api;

import com.mall.aichat.domain.Nl2sqlBusinessRule;
import com.mall.aichat.service.INl2sqlBusinessRuleService;
import com.mall.common.core.domain.R;
import com.mall.common.security.annotation.InnerAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NL2SQL业务规则内部API（供远程服务Feign调用）
 * <p>
 * 供 mall-ai-mcp-server 等远程服务通过 Feign 调用，获取语义层业务规则定义。
 * 替代调用方直接对 mall_ai 库执行 SELECT * FROM nl2sql_business_rule 的裸 SQL 查询。
 */
@RestController
@RequestMapping("/api/nl2sql/business-rule")
public class Nl2sqlBusinessRuleApi {

    @Autowired
    private INl2sqlBusinessRuleService nl2sqlBusinessRuleService;

    /**
     * 查询启用的NL2SQL业务规则列表
     *
     * @return 业务规则列表
     */
    @GetMapping("/list")
    @InnerAuth
    public R<List<Nl2sqlBusinessRule>> list() {
        Nl2sqlBusinessRule query = new Nl2sqlBusinessRule();
        query.setEnabled("1");
        return R.ok(nl2sqlBusinessRuleService.selectNl2sqlBusinessRuleList(query));
    }
}