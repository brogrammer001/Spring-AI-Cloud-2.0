package com.mall.aichat.api;

import com.mall.aichat.domain.Nl2sqlEval;
import com.mall.aichat.service.INl2sqlEvalService;
import com.mall.common.core.domain.R;
import com.mall.common.security.annotation.InnerAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NL2SQL黄金评测集内部API（供远程服务Feign调用）
 * <p>
 * 供 mall-ai-mcp-server 等远程服务通过 Feign 调用，获取黄金评测集用例。
 * 替代调用方直接对 mall_ai 库执行 SELECT * FROM nl2sql_eval 的裸 SQL 查询。
 */
@RestController
@RequestMapping("/api/nl2sql/eval")
public class Nl2sqlEvalApi {

    @Autowired
    private INl2sqlEvalService nl2sqlEvalService;

    /**
     * 查询启用的NL2SQL黄金评测集列表
     *
     * @return 评测集列表
     */
    @GetMapping("/list")
    @InnerAuth
    public R<List<Nl2sqlEval>> list() {
        Nl2sqlEval query = new Nl2sqlEval();
        query.setEnabled("1");
        return R.ok(nl2sqlEvalService.selectNl2sqlEvalList(query));
    }
}