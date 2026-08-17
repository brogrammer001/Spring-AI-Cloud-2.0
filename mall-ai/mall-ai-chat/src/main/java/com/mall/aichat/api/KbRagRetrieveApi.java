package com.mall.aichat.api;

import com.mall.aichat.service.impl.RagRetrieveContextService;
import com.mall.common.core.domain.R;
import com.mall.common.security.annotation.InnerAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库RAG检索内部API（供远程服务Feign调用）
 * <p>
 * 对标 Dify Knowledge Retrieve API，提供统一的向量检索与 Reranker 重排序端点。
 * 业务模块通过 kbType 参数区分不同知识库类型：
 * - kbType=10: ChatAgent 通用知识
 * - kbType=20: NL2SQL 表结构专业知识
 * <p>
 * 所有 RAG 检索逻辑统一在 chat 服务端执行，调用方无需持有 VectorStore 配置。
 */
@RestController
@RequestMapping("/api/kb")
public class KbRagRetrieveApi {

    @Autowired
    private RagRetrieveContextService ragRetrieveContextService;

    /**
     * 按知识库类型检索相关文档片段
     *
     * @param question 用户自然语言问题
     * @param kbType   知识库类型（10-通用知识, 20-表结构专业知识, ...）
     * @return 检索到的文本片段，未检索到返回空字符串
     */
    @GetMapping("/retrieve")
    @InnerAuth
    public R<String> retrieve(@RequestParam("question") String question,
                               @RequestParam("kbType") String kbType) {
        return R.ok(ragRetrieveContextService.retrieveContext(question, kbType));
    }
}
