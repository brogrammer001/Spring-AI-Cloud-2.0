package com.mall.aichat.advisor;

import com.mall.aichat.config.AgentEventSinkManager;
import com.mall.aichat.constant.ChatConstants;
import com.mall.aichat.domain.KbDocument;
import com.mall.aichat.service.IKbDocumentService;
import com.mall.aichat.service.impl.RerankerService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.*;
import java.util.stream.Collectors;

import static com.mall.aichat.constant.ChatConstants.*;

/**
 * 知识库上下文查询 Advisor
 * <p>
 * 参考 {@link VectorStoreChatMemoryAdvisor} 的 Advisor 模式，将知识库查询逻辑收敛到
 * 请求链路中：在进入模型之前检索知识库相关片段，注入系统提示词，并通过
 * {@link AgentEventSinkManager} 推送 RAG 检索状态事件（start / success / empty）。
 * <p>
 * 检索策略（与 RagRetrieveContextService 对齐）：
 * 1. 知识库前置检查：查询 kbType 下是否有启用的知识库，无则跳过向量检索
 * 2. 反向匹配：获取 kbType 下所有文档，在内存中按 knowledgeId 过滤 → 检查用户消息是否包含某个 tag
 * 3. 模糊匹配：若反向匹配未命中，将 question 拆分为关键词，在内存中检查 tags 是否包含任一关键词
 * 4. 全量降级：若标签匹配均未命中，退化为仅 knowledgeId 过滤的全量检索
 * 5. 向量检索：tags + knowledgeId 双重过滤
 * 6. Reranker 重排序：对向量检索结果二次排序，返回最相关的片段
 */
public class RagContextQueryAdvisor implements BaseChatMemoryAdvisor {

    private final Logger log = LoggerFactory.getLogger(RagContextQueryAdvisor.class);

    /** Reranker 启用时最终返回的 TopN */
    private static final int TOP_N = 3;

    /** 默认知识库类型：10-通用知识 */
    private static final String DEFAULT_KB_TYPE = "10";

    private final int order;

    private final Scheduler scheduler;

    private final VectorStore knowledgeVectorStore;

    private final IKbDocumentService kbDocumentService;

    private final RerankerService rerankerService;

    private final AgentEventSinkManager agentEventSinkManager;

    private RagContextQueryAdvisor(int order,
                                   Scheduler scheduler,
                                   VectorStore knowledgeVectorStore,
                                   IKbDocumentService kbDocumentService,
                                   RerankerService rerankerService,
                                   AgentEventSinkManager agentEventSinkManager) {
        Assert.notNull(scheduler, "scheduler cannot be null");
        Assert.notNull(knowledgeVectorStore, "knowledgeVectorStore cannot be null");
        Assert.notNull(kbDocumentService, "kbDocumentService cannot be null");
        Assert.notNull(rerankerService, "rerankerService cannot be null");
        Assert.notNull(agentEventSinkManager, "agentEventSinkManager cannot be null");
        this.order = order;
        this.scheduler = scheduler;
        this.knowledgeVectorStore = knowledgeVectorStore;
        this.kbDocumentService = kbDocumentService;
        this.rerankerService = rerankerService;
        this.agentEventSinkManager = agentEventSinkManager;
    }

    public static Builder builder(VectorStore knowledgeVectorStore,
                                  IKbDocumentService kbDocumentService,
                                  RerankerService rerankerService,
                                  AgentEventSinkManager agentEventSinkManager) {
        return new Builder(knowledgeVectorStore, kbDocumentService, rerankerService, agentEventSinkManager);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    /**
     * 进入模型之前：检索知识库相关片段注入系统提示词，并推送 RAG 检索状态事件。
     * <p>检索是增强项而非必需项：向量库不可用或检索失败时降级为无知识继续对话，不中断整个请求</p>
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        String conversationId = getConversationId(request.context());
        String query = Objects.requireNonNullElse(request.prompt().getUserMessage().getText(), "");
        String kbType = getKbType(request.context());

        // 推送 RAG 检索开始事件
        agentEventSinkManager.emitRagRetrieve(conversationId, ChatConstants.RAG_START);

        // 检索知识库上下文（失败降级为空，不影响主对话链路）
        String relevantContext = retrieveContext(query, kbType);

        if (StringUtils.hasText(relevantContext)) {
            agentEventSinkManager.emitRagRetrieve(conversationId, ChatConstants.RAG_SUCCESS);
        } else {
            agentEventSinkManager.emitRagRetrieve(conversationId, ChatConstants.RAG_EMPTY);
        }

        // 将检索到的知识拼接到系统提示词
        SystemMessage systemMessage = request.prompt().getSystemMessage();
        String augmentedSystemText = systemMessage.getText() + ChatConstants.KNOWLEDGE_PREFIX + relevantContext;

        return request.mutate()
            .prompt(request.prompt().augmentSystemMessage(augmentedSystemText))
            .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Scheduler scheduler = this.getScheduler();
        return Mono.just(chatClientRequest)
            .publishOn(scheduler)
            .map(request -> this.before(request, streamAdvisorChain))
            .flatMapMany(streamAdvisorChain::nextStream)
            .transform(flux -> new ChatClientMessageAggregator()
                .aggregateChatClientResponse(flux, response -> this.after(response, streamAdvisorChain)));
    }

    /**
     * 根据问题和知识库类型从向量库检索相关文档片段
     * <p>
     * 检索策略（知识库前置检查 → 反向匹配 → 模糊匹配 → 全量降级 → 向量检索 → Reranker 重排序）：
     * 1. 知识库前置检查：查询 kbType 下是否有启用的知识库，无则跳过向量检索
     * 2. 反向匹配：获取 kbType 下所有文档，在内存中按 knowledgeId 过滤 → 匹配 question 包含的 tag → 找出所有相似的 KbDocument
     * 3. 模糊匹配：若反向匹配未命中，将 question 拆分为关键词，在内存中检查 tags 是否包含任一关键词
     * 4. 全量降级：若标签匹配均未命中，退化为仅 knowledgeId 过滤的全量检索
     * 5. 向量检索：从匹配文档获取 getKnowledgeId，用 knowledgeId IN (...) 过滤向量库
     * 6. 重排序：对向量检索结果用 Reranker 二次排序，返回最相关的片段
     *
     * @param question 用户问题
     * @param kbType   知识库类型（10-通用知识, 20-表结构专业知识, ...）
     * @return 检索到的文本片段，未检索到返回空字符串
     */
    private String retrieveContext(String question, String kbType) {
        // 前置校验
        if (!validateInput(question, kbType)) {
            return "";
        }

        // ========== 第一步：标签匹配 → 找出所有相似 KbDocument，获取精确 tags 和 knowledgeId ==========
        MatchResult matchResult = matchDocumentsByTags(question, kbType);
        if (!matchResult.hasKnowledgeIds()) {
            log.info("[RAG检索] kbType={} 无启用的知识库，跳过检索", kbType);
            return "";
        }

        // ========== 第二步：向量检索（knowledgeId 过滤） ==========
        List<Document> results = vectorSearch(question, matchResult);
        if (results.isEmpty()) {
            log.info("[RAG检索] kbType={} 向量检索未命中任何片段", kbType);
            return "";
        }

        // ========== 第三步：Reranker 重排序 ==========
        List<Document> finalResults = rerankAndTrim(question, results);

        return finalResults.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 前置校验：向量库配置、问题、知识库类型
     */
    private boolean validateInput(String question, String kbType) {
        if (knowledgeVectorStore == null) {
            log.warn("[RAG检索] 知识库向量库未配置，跳过检索");
            return false;
        }
        if (question == null || question.trim().isEmpty()) {
            log.warn("[RAG检索] question为空，跳过检索");
            return false;
        }
        if (kbType == null || kbType.trim().isEmpty()) {
            log.warn("[RAG检索] kbType为空，跳过检索");
            return false;
        }
        return true;
    }

    /**
     * 第一步：标签匹配 → 找出所有相似的 KbDocument
     * <p>
     * 匹配策略（三级降级）：
     * 1. 知识库前置检查：先查 kbType 下是否有启用的知识库，无则直接返回空（跳过向量检索）
     * 2. 反向匹配：获取 kbType 下所有文档，在内存中按 knowledgeId 过滤 → 检查 question 是否包含某个 tag（准确率100%）
     * 3. 模糊匹配：若反向匹配未命中，将 question 拆分为关键词，在内存中检查 tags 是否包含任一关键词
     * 4. 全量降级：若标签匹配均未命中，退化为仅 knowledgeId 过滤的全量检索
     * <p>
     * 匹配成功后，提取精确的 tags 值（向量库 metadata 中的完整字符串）和 knowledgeId
     *
     * @return 匹配结果（包含 tags 过滤值和 knowledgeIds）
     */
    private MatchResult matchDocumentsByTags(String question, String kbType) {
        // ========== 第一步：关联查询 kbType 下所有启用知识库的文档（一次SQL） ==========
        List<KbDocument> filteredDocs = kbDocumentService.selectDocumentsByKbType(kbType, "0");
        if (filteredDocs == null || filteredDocs.isEmpty()) {
            log.info("[RAG检索] kbType={} 无启用的知识库，跳过检索", kbType);
            return new MatchResult(null, null);
        }

        // 提取知识库ID集合（用于全量降级场景）
        Set<String> kbIdSet = filteredDocs.stream()
            .map(KbDocument::getKnowledgeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        int docCount = filteredDocs.size();
        log.info("[RAG检索] kbType={} 查询到{}个启用的知识库，{}个文档", kbType, kbIdSet.size(), docCount);

        // ========== 第三步：反向匹配（纯内存计算） ==========
        List<KbDocument> matchedDocs = matchDocsByReverseTag(question, filteredDocs);
        if (!matchedDocs.isEmpty()) {
            return buildMatchResult(matchedDocs, "反向匹配", kbType);
        }

        // ========== 第四步：模糊匹配（纯内存过滤，不走SQL LIKE） ==========
        log.info("[RAG检索] kbType={} 反向匹配未命中，尝试基于内存的模糊匹配", kbType);
        List<KbDocument> fuzzyDocs = matchDocsByFuzzyMatch(question, filteredDocs);
        if (!fuzzyDocs.isEmpty()) {
            return buildMatchResult(fuzzyDocs, "模糊匹配", kbType);
        }

        // ========== 第五步：全量降级 ==========
        log.info("[RAG检索] kbType={} 标签匹配均未命中，降级为全量检索（仅knowledgeId过滤）", kbType);
        String[] allKbIds = kbIdSet.toArray(new String[0]);
        return new MatchResult(null, allKbIds);
    }

    /**
     * 模糊匹配：将 question 拆分为关键词，在内存中检查 tags 是否包含任一关键词
     * <p>
     * 替代原来的 SQL LIKE 查询，纯内存计算，零SQL开销。
     * 关键词拆分策略：按中英文逗号、空格、常见标点符号拆分。
     *
     * @param question 用户问题
     * @param docs     待过滤的文档列表（已按 knowledgeId 过滤）
     * @return 匹配的文档列表（tags 包含任一关键词）
     */
    private List<KbDocument> matchDocsByFuzzyMatch(String question, List<KbDocument> docs) {
        if (docs == null || docs.isEmpty() || question == null || question.trim().isEmpty()) {
            return List.of();
        }

        // 按中英文逗号、空格、常见标点拆分关键词
        String[] keywords = question.split("[,，、\\s]+");

        List<KbDocument> matched = new ArrayList<>();
        Set<String> matchedDocIds = new HashSet<>();

        for (KbDocument doc : docs) {
            if (doc.getTags() == null || doc.getTags().trim().isEmpty()) {
                continue;
            }

            String docTagsLower = doc.getTags().toLowerCase();
            for (String keyword : keywords) {
                String trimmed = keyword.trim().toLowerCase();
                // 关键词至少2个字符才匹配，避免单字误匹配
                if (trimmed.length() >= 2 && docTagsLower.contains(trimmed)) {
                    if (matchedDocIds.add(doc.getId())) {
                        matched.add(doc);
                        log.info("[RAG检索] 模糊匹配命中: keyword='{}' ∈ tags='{}'", keyword, doc.getTags());
                    }
                    break; // 一个关键词匹配即可，避免重复添加
                }
            }
        }
        return matched;
    }

    /**
     * 从匹配的文档中提取 tags 和 knowledgeId，构建 MatchResult
     */
    private MatchResult buildMatchResult(List<KbDocument> docs, String matchType, String kbType) {
        Set<String> tagSet = new LinkedHashSet<>();
        Set<String> kbIdSet = new LinkedHashSet<>();
        for (KbDocument doc : docs) {
            if (doc.getTags() != null && !doc.getTags().isEmpty()) {
                // 把整个 tags 字符串作为过滤值（向量库 metadata 中的完整值）
                tagSet.add(doc.getTags());
            }
            if (doc.getKnowledgeId() != null) {
                kbIdSet.add(doc.getKnowledgeId());
            }
        }
        String[] tagValues = tagSet.toArray(new String[0]);
        String[] knowledgeIds = kbIdSet.toArray(new String[0]);
        log.info("[RAG检索] {}成功，命中{}个文档，{}个tags值，{}个知识库",
            matchType, docs.size(), tagValues.length, knowledgeIds.length);
        return new MatchResult(tagValues, knowledgeIds);
    }

    /**
     * 反向匹配：检查 question 是否包含某个 tag
     * <p>
     * 相比正向关键词提取（extractKeywords）的优势：
     * - 准确率100%：直接字符串包含检查，无分词误差
     * - 零延迟：纯内存计算，无 LLM/NLP 调用
     * - 零依赖：无需 Lucene/HanLP/jieba 等分词库
     *
     * @param question 用户问题
     * @param docs     所有带 tags 的文档列表
     * @return 匹配的文档列表（question 包含某个 tag）
     */
    private List<KbDocument> matchDocsByReverseTag(String question, List<KbDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        List<KbDocument> matched = new ArrayList<>();
        Set<String> matchedDocIds = new HashSet<>(); // 防止重复添加同一文档

        for (KbDocument doc : docs) {
            if (doc.getTags() == null || doc.getTags().trim().isEmpty()) {
                matched.add(doc);
                continue;
            }

            // 按逗号分割 tags（兼容中英文逗号）
            String[] tagArray = doc.getTags().split("[,，]");
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                // tag 至少2个字符才匹配，避免单字误匹配
                if (trimmedTag.length() >= 2 && question.contains(trimmedTag)) {
                    // 防止重复添加同一文档
                    if (matchedDocIds.add(doc.getId())) {
                        matched.add(doc);
                        log.info("[RAG检索] 反向匹配命中: tag='{}' ∈ question='{}'", trimmedTag, question);
                    }
                    break; // 一个 tag 匹配即可，避免重复添加
                }
            }
        }
        return matched;
    }

    /**
     * 第二步：向量检索（knowledgeId 过滤）
     * <p>
     * 根据是否启用 Reranker 动态调整检索参数：
     * - 启用 Reranker：topK=10, 相似度阈值=0.5（召回更多候选，交给Reranker精排）
     * - 未启用 Reranker：topK=3, 相似度阈值=0.7（直接取高相似度片段）
     */
    private List<Document> vectorSearch(String question, MatchResult matchResult) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        Filter.Expression expression = b.in(KNOWLEDGE_ID, matchResult.getKnowledgeIds()).build();
        log.info("[RAG检索] 启用单重过滤：knowledgeId IN {}", matchResult.getKnowledgeIds().length);

        boolean rerankEnabled = rerankerService.isEnabled();
        int retrieveTopK = rerankEnabled ? 10 : TOP_N;
        double similarityThreshold = rerankEnabled ? 0.5 : 0.7;

        SearchRequest request = SearchRequest.builder()
            .query(question)
            .topK(retrieveTopK)
            .similarityThreshold(similarityThreshold)
            .filterExpression(expression)
            .build();

        return knowledgeVectorStore.similaritySearch(request);
    }

    /**
     * 第三步：Reranker 重排序 + 截取 TopN
     */
    private List<Document> rerankAndTrim(String question, List<Document> results) {
        boolean rerankEnabled = rerankerService.isEnabled();
        // 取前 min(TOP_N, results.size()) 条，避免越界
        int trimTo = Math.min(TOP_N, results.size());

        if (rerankEnabled && results.size() > 1) {
            List<String> docTexts = results.stream().map(Document::getText).toList();
            List<Integer> rerankedIndices = rerankerService.rerank(question, docTexts);

            if (!rerankedIndices.isEmpty()) {
                // 按 Reranker 返回的索引顺序组装结果
                List<Document> reranked = new ArrayList<>();
                for (int idx : rerankedIndices) {
                    if (idx < results.size()) {
                        reranked.add(results.get(idx));
                    }
                }
                // 只保留前 TOP_N 条
                if (reranked.size() > TOP_N) {
                    reranked = new ArrayList<>(reranked.subList(0, TOP_N));
                }
                log.info("[RAG检索] 向量检索{}条 → 重排序后取{}条", results.size(), reranked.size());
                return reranked;
            } else {
                log.warn("[RAG检索] Reranker降级，使用向量检索前{}条", trimTo);
            }
        }

        return results.subList(0, trimTo);
    }

    /**
     * 从 context 中获取知识库类型，默认 10（通用知识）
     */
    private String getKbType(Map<String, @Nullable Object> context) {
        Object fromCtx = context.get(CTX_KB_TYPE);
        if (fromCtx != null) {
            return fromCtx.toString();
        }
        return DEFAULT_KB_TYPE;
    }

    /**
     * 匹配结果封装：包含 tags 过滤值和 knowledgeIds
     */
    private static class MatchResult {
        private final String[] tagValues;
        private final String[] knowledgeIds;

        MatchResult(String[] tagValues, String[] knowledgeIds) {
            this.tagValues = tagValues;
            this.knowledgeIds = knowledgeIds != null ? knowledgeIds : new String[0];
        }

        boolean hasTagValues() {
            return tagValues != null && tagValues.length > 0;
        }

        boolean hasKnowledgeIds() {
            return knowledgeIds.length > 0;
        }

        String[] getTagValues() {
            return tagValues;
        }

        String[] getKnowledgeIds() {
            return knowledgeIds;
        }
    }

    /**
     * Builder for RagContextQueryAdvisor.
     */
    public static final class Builder {
        private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;
        private final VectorStore knowledgeVectorStore;
        private final IKbDocumentService kbDocumentService;
        private final RerankerService rerankerService;
        private final AgentEventSinkManager agentEventSinkManager;

        Builder(VectorStore knowledgeVectorStore,
                IKbDocumentService kbDocumentService,
                RerankerService rerankerService,
                AgentEventSinkManager agentEventSinkManager) {
            this.knowledgeVectorStore = knowledgeVectorStore;
            this.kbDocumentService = kbDocumentService;
            this.rerankerService = rerankerService;
            this.agentEventSinkManager = agentEventSinkManager;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public RagContextQueryAdvisor build() {
            return new RagContextQueryAdvisor(this.order, this.scheduler,
                this.knowledgeVectorStore, this.kbDocumentService,
                this.rerankerService, this.agentEventSinkManager);
        }
    }
}