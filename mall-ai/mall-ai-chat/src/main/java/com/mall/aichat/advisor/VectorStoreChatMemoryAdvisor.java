package com.mall.aichat.advisor;

import com.mall.aichat.config.NacosPromptRegistry;
import com.mall.aichat.domain.AiMemoryFact;
import com.mall.aichat.domain.ExtractedMemory;
import com.mall.aichat.domain.MemoryOperation;
import com.mall.aichat.domain.MemoryType;
import com.mall.aichat.service.IAiMemoryFactService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.converter.BeanOutputConverter;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mall.aichat.constant.ChatConstants.*;

public class VectorStoreChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private final Logger log = LoggerFactory.getLogger(VectorStoreChatMemoryAdvisor.class);

    private static final int DEFAULT_CTX_CHAT_MEMORY_TOP_K = 5;
    private static final long DEFAULT_MEMORY_TTL_MS = 30L * 24 * 60 * 60 * 1000;
    private static final int MAX_EXTRACT_INPUT_LENGTH = 400;
    private static final int DECISION_CANDIDATE_TOP_K = 3;
    private static final double DECISION_SIMILARITY_THRESHOLD = 0.75;
    private static final int MAX_MEMORY_TEXT_LENGTH = 100;
    private static final int MAX_MEMORIES_PER_TURN = 5;
    private static final double DEFAULT_IMPORTANCE = CHAT_MEMORY_DEFAULT_IMPORTANCE;
    private static final int MAX_ASSISTANT_TEXT_LENGTH = 300;
    private static final int USER_LOCK_STRIPES = 64;
    private static final String DEFAULT_AGENT_ID = "default";
    private static final String CTX_CURRENT_USER_TEXT = "_vector_memory_current_user_text";
    /** before() 注入的记忆快照，供 after() 提取时排除，避免 AI 复述被抽成新记忆 */
    private static final String CTX_RECALLED_MEMORY = "_vector_memory_recalled_snapshot";

    private static final Pattern PATTERN_PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern PATTERN_ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");
    private static final Pattern PATTERN_BANK_CARD = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");
    private static final String MASK = "***";

    private final int defaultTopK;
    private final int order;
    private final Scheduler scheduler;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final long memoryTtlMs;
    private final NacosPromptRegistry promptRegistry;
    /** MySQL 权威存储：先落库拿到自增 id，再以该 id 写 Weaviate 索引 */
    private final IAiMemoryFactService memoryFactService;

    /** 结构化输出转换器：替代全部手写 JSON 修复/解析 */
    private final BeanOutputConverter<ExtractionResult> converter =
        new BeanOutputConverter<>(ExtractionResult.class);
    private final BeanOutputConverter<Verdict> verdictConverter =
        new BeanOutputConverter<>(Verdict.class);

    /** 用户级条带锁：同一用户并发写记忆时只执行一轮，天然限流 */
    private final Lock[] userLocks = IntStream.range(0, USER_LOCK_STRIPES)
        .mapToObj(i -> new ReentrantLock())
        .toArray(Lock[]::new);

    private VectorStoreChatMemoryAdvisor(int defaultTopK,
                                       int order, Scheduler scheduler, VectorStore vectorStore,
                                       ChatClient chatClient, long memoryTtlMs, NacosPromptRegistry promptRegistry,
                                       IAiMemoryFactService memoryFactService) {
        Assert.isTrue(defaultTopK > 0, "topK must be greater than 0");
        Assert.notNull(scheduler, "scheduler cannot be null");
        Assert.notNull(vectorStore, "vectorStore cannot be null");
        Assert.notNull(chatClient, "chatClient cannot be null");
        Assert.notNull(promptRegistry, "promptRegistry cannot be null");
        Assert.notNull(memoryFactService, "memoryFactService cannot be null");
        Assert.isTrue(memoryTtlMs >= 0, "memoryTtlMs must be >= 0");
        this.defaultTopK = defaultTopK;
        this.order = order;
        this.scheduler = scheduler;
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.memoryTtlMs = memoryTtlMs;
        this.promptRegistry = promptRegistry;
        this.memoryFactService = memoryFactService;
    }

    public static Builder builder(VectorStore vectorStore, ChatClient chatClient,
                                  NacosPromptRegistry promptRegistry, IAiMemoryFactService memoryFactService) {
        return new Builder(vectorStore, chatClient, promptRegistry, memoryFactService);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        String conversationId = getConversationId(request.context());
        String userId = getUserId(request.context());

        // 匿名用户不读写长期记忆，避免所有未登录用户共享同一记忆空间
        if (isAnonymous(userId)) {
            return request;
        }

        String query = Objects.requireNonNullElse(request.prompt().getUserMessage().getText(), "");
        int topK = getChatMemoryTopK(request.context());

        // 将用户消息存入 context，供 after() 阶段做完整轮次记忆提取
        request.context().put(CTX_CURRENT_USER_TEXT, query);

        Filter.Expression expression = activeMemoryFilter(new FilterExpressionBuilder(),
            userId, System.currentTimeMillis());
        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .filterExpression(expression)
            .build();

        List<Document> documents;
        try {
            documents = this.vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.warn("会话[{}] 长期记忆检索失败，降级为无记忆回答", conversationId, e);
            documents = List.of();
        }

        List<Document> ranked = rankByRecency(documents);
        String longTermMemory = ranked.stream()
            .map(doc -> "[" + escapeXml(doc.getText()) + "]")
            .collect(Collectors.joining(System.lineSeparator()));

        if (StringUtils.hasText(longTermMemory)) {
            // 快照存入 context，after() 提取记忆时排除，防止 AI 复述被再次抽成新记忆
            request.context().put(CTX_RECALLED_MEMORY, longTermMemory);
            SystemMessage systemMessage = request.prompt().getSystemMessage();
            String augmentedSystemText = promptRegistry.render("VectorStoreChatMemoryPrompt",
                Map.of("instructions", systemMessage.getText(), "long_term_memory", longTermMemory));
            return request.mutate()
                .prompt(request.prompt().augmentSystemMessage(augmentedSystemText))
                .build();
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        Map<String, @Nullable Object> context = chatClientResponse.context();
        String conversationId = getConversationId(context);
        String userId = getUserId(context);

        // 匿名用户不写记忆（读写两端一致拦截）
        if (isAnonymous(userId)) {
            return chatClientResponse;
        }

        Object userTextObj = context.get(CTX_CURRENT_USER_TEXT);
        String userText = userTextObj != null ? userTextObj.toString() : null;
        if (!StringUtils.hasText(userText)) {
            return chatClientResponse;
        }

        String assistantText = null;
        if (chatClientResponse.chatResponse() != null
            && chatClientResponse.chatResponse().getResult() != null) {
            assistantText = chatClientResponse.chatResponse().getResult().getOutput().getText();
        }

        Object recalledObj = context.get(CTX_RECALLED_MEMORY);
        String recalledSnapshot = recalledObj != null ? recalledObj.toString() : null;
        storeMemoryAsync(userText, assistantText, userId, conversationId, recalledSnapshot);
        return chatClientResponse;
    }

    private void storeMemoryAsync(String userText, @Nullable String assistantText,
                                  String userId, String conversationId,
                                  @Nullable String recalledSnapshot) {
        Mono.fromRunnable(() -> storeMemory(userText, assistantText, userId, conversationId, recalledSnapshot))
            .subscribeOn(this.scheduler)
            .subscribe(_ -> {
            }, e -> log.error("会话[{}] 记忆异步存储失败", conversationId, e));
    }

    private void storeMemory(String userText, @Nullable String assistantText,
                             String userId, String conversationId,
                             @Nullable String recalledSnapshot) {
        if (!StringUtils.hasText(userText) || isAnonymous(userId)) {
            return;
        }
        // 同一用户并发两轮时只执行一轮，避免各自基于过期快照裁决出重复记忆
        Lock lock = userLocks[Math.floorMod(Objects.hashCode(userId), userLocks.length)];
        if (!lock.tryLock()) {
            log.info("会话[{}] 记忆管道忙（同一用户并发写），跳过本轮", conversationId);
            return;
        }
        try {
            List<ExtractedMemory> extracted = extractMemories(userText, assistantText, recalledSnapshot, conversationId);
            if (extracted.isEmpty()) {
                log.debug("会话[{}] 本轮无实质记忆", conversationId);
                return;
            }
            for (ExtractedMemory memory : extracted) {
                try {
                    decideAndApply(memory, userId, conversationId);
                } catch (Exception e) {
                    log.warn("会话[{}] 记忆[{}]处理失败，跳过", conversationId, memory.content(), e);
                }
            }
        } catch (Exception e) {
            log.warn("会话[{}] 记忆存储失败，本轮放弃", conversationId, e);
        } finally {
            lock.unlock();
        }
    }

    private List<ExtractedMemory> extractMemories(String userText, @Nullable String assistantText,
                                                  @Nullable String recalledSnapshot,
                                                  String conversationId) {
        StringBuilder input = new StringBuilder();
        input.append("【用户消息】").append(truncate(userText, MAX_EXTRACT_INPUT_LENGTH));
        if (StringUtils.hasText(assistantText)) {
            input.append(System.lineSeparator())
                .append("【AI回复】")
                .append(truncate(assistantText, MAX_ASSISTANT_TEXT_LENGTH));
        }
        // 防反馈回路：已注入的长期记忆不再重复抽取
        input.append(System.lineSeparator())
            .append("【已知记忆（勿重复抽取）】")
            .append(StringUtils.hasText(recalledSnapshot) ? recalledSnapshot : "无");

        String output;
        try {
            String systemPrompt = promptRegistry.render("ExtractInstructionPrompt", Map.of(
                "schema", converter.getJsonSchema(),
                "date", LocalDate.now().toString()));
            output = this.chatClient.prompt()
                .system(systemPrompt)
                .user(input.toString())
                .call().content();
        } catch (Exception e) {
            log.warn("会话[{}] 提取 LLM 调用失败，本轮不写记忆", conversationId, e);
            return List.of();
        }
        return parseExtractedMemories(output, conversationId);
    }

    private List<ExtractedMemory> parseExtractedMemories(@Nullable String output, String conversationId) {
        if (!StringUtils.hasText(output)) {
            return List.of();
        }
        try {
            ExtractionResult result = this.converter.convert(output);
            if (result.facts() == null || result.facts().isEmpty()) {
                return List.of();
            }
            List<ExtractedMemory> memories = new ArrayList<>();
            for (ExtractedFact fact : result.facts()) {
                if (memories.size() >= MAX_MEMORIES_PER_TURN) {
                    break;
                }
                String content = fact.content() != null ? fact.content().strip() : "";
                if (!isValidMemoryText(content)) {
                    log.debug("会话[{}] 提取的记忆不合规，跳过: {}", conversationId, content);
                    continue;
                }
                memories.add(new ExtractedMemory(content, MemoryType.fromLower(fact.type()),
                    normalizeImportance(fact.importance())));
            }
            return memories;
        } catch (Exception e) {
            log.warn("会话[{}] 提取输出解析失败，本轮不写记忆: {}", conversationId, output, e);
            return List.of();
        }
    }

    private void decideAndApply(ExtractedMemory memory,
                                String userId, String conversationId) {
        List<Document> candidates = searchDecisionCandidates(memory.content(), userId, conversationId);

        if (candidates.isEmpty()) {
            writeMemory(memory.content(), memory.type(), memory.importance(), userId, conversationId, null);
            return;
        }

        // 完全相同：同 id 覆写续期，一次写入完成，不再写新删旧
        for (Document candidate : candidates) {
            String candidateText = candidate.getText();
            if (candidateText != null && candidateText.strip().equals(memory.content())) {
                writeMemory(memory.content(), memory.type(), memory.importance(), userId, conversationId, candidate.getId());
                log.debug("会话[{}] 记忆完全相同，同 id 续期: {}", conversationId, memory.content());
                return;
            }
        }

        MemoryDecision decision = decide(memory, candidates, conversationId);
        if (decision == null) {
            writeMemory(memory.content(), memory.type(), memory.importance(), userId, conversationId, null);
            return;
        }

        switch (decision.op()) {
            case ADD -> writeMemory(memory.content(), memory.type(), memory.importance(),
                userId, conversationId, null);
            case UPDATE -> writeMemory(decision.content(), memory.type(), memory.importance(),
                userId, conversationId, decision.targetId());
            case DELETE -> deleteMemory(decision.targetId(), conversationId);
            case NOOP -> log.debug("会话[{}] 记忆重复，跳过: {}", conversationId, memory.content());
        }
    }

    private List<Document> searchDecisionCandidates(String memoryText, String userId, String conversationId) {
        Filter.Expression expression = activeMemoryFilter(new FilterExpressionBuilder(),
            userId, System.currentTimeMillis());
        try {
            // 不同向量库对 similarityThreshold 支持不一致：多召回一倍，再按 score 事后过滤
            List<Document> candidates = this.vectorStore.similaritySearch(SearchRequest.builder()
                .query(memoryText)
                .topK(DECISION_CANDIDATE_TOP_K * 2)
                .filterExpression(expression)
                .build());
            return candidates.stream()
                .filter(doc -> doc.getScore() == null
                    || doc.getScore() >= DECISION_SIMILARITY_THRESHOLD)
                .limit(DECISION_CANDIDATE_TOP_K)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("会话[{}] 决策阶段检索失败，按新增写入", conversationId, e);
            return List.of();
        }
    }

    private record MemoryDecision(MemoryOperation op, String targetId, String content) {
    }

    private @Nullable MemoryDecision decide(ExtractedMemory memory, List<Document> candidates,
                                            String conversationId) {
        try {
            StringBuilder candidateText = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                Document doc = candidates.get(i);
                Object typeMeta = doc.getMetadata().get(CHAT_MEMORY_TYPE);
                String type = typeMeta != null ? typeMeta.toString() : MemoryType.FACT.lower();
                candidateText.append("  - index=").append(i)
                    .append(": ").append(doc.getText())
                    .append(" (type=").append(type).append(")\n");
            }
            String userContent = "新记忆：" + memory.content()
                + " (type=" + memory.type().lower() + ")\n"
                + "已存在相似记忆：\n" + candidateText
                + "请判定如何处理新记忆。";

            String output = this.chatClient.prompt()
                .system(promptRegistry.get("DecideInstructionPrompt"))
                .user(userContent)
                .call().content();

            return toDecision(this.verdictConverter.convert(output), memory, candidates, conversationId);
        } catch (Exception e) {
            log.warn("会话[{}] 决策 LLM 调用/解析失败，按新增写入", conversationId, e);
            return null;
        }
    }

    /**
     * 裁决结果转操作指令：targetIndex 越界、候选内容与 oldMemory 不一致时一律降级 ADD，
     * 防止 LLM 幻觉出的 id 误删/误改记忆。
     */
    private @Nullable MemoryDecision toDecision(@Nullable Verdict verdict, ExtractedMemory fallback,
                                                List<Document> candidates, String conversationId) {
        if (verdict == null || !StringUtils.hasText(verdict.event())) {
            return null;
        }
        String event = verdict.event().strip().toUpperCase();
        if ("NONE".equals(event)) {
            event = MemoryOperation.NOOP.name();
        }
        MemoryOperation op;
        try {
            op = MemoryOperation.valueOf(event);
        } catch (IllegalArgumentException e) {
            log.warn("会话[{}] 决策输出事件非法: {}，按新增处理", conversationId, event);
            return null;
        }

        String content = StringUtils.hasText(verdict.content())
            ? verdict.content().strip() : fallback.content();
        if (op == MemoryOperation.ADD || op == MemoryOperation.NOOP) {
            return new MemoryDecision(op, null, content);
        }

        if (verdict.targetIndex() == null
            || verdict.targetIndex() < 0 || verdict.targetIndex() >= candidates.size()) {
            log.warn("会话[{}] 决策 targetIndex 越界: {}，按新增处理", conversationId, verdict.targetIndex());
            return null;
        }
        Document target = candidates.get(verdict.targetIndex());
        if (StringUtils.hasText(verdict.oldMemory())) {
            String targetText = target.getText() != null ? target.getText() : "";
            if (!normalizeText(targetText).equals(normalizeText(verdict.oldMemory()))) {
                log.warn("会话[{}] 决策 oldMemory 与候选不一致，按新增处理", conversationId);
                return null;
            }
        }
        if (op == MemoryOperation.UPDATE && !isValidMemoryText(content)) {
            log.warn("会话[{}] UPDATE 输出不合规，按新增处理", conversationId);
            return null;
        }
        return new MemoryDecision(op, target.getId(), content);
    }

    private String normalizeText(String text) {
        return text.replaceAll("\\s+", "").toLowerCase();
    }

    /**
     * 写入记忆：MySQL 为权威存储，先落库拿到自增 id，再以同 id 写 Weaviate 索引。
     * existingId 非空时更新原记录（同 id 覆写），一次写入完成更新/续期。
     */
    private void writeMemory(String text, MemoryType type, double importance,
                             String userId, String conversationId, @Nullable String existingId) {
        String maskedText = maskSensitiveInfo(text);
        AiMemoryFact fact = saveFact(existingId, maskedText, type, importance, userId, conversationId);
        if (fact == null || fact.getId() == null) {
            log.warn("会话[{}] 记忆落库失败，跳过向量写入: {}", conversationId, maskedText);
            return;
        }

        String factId = String.valueOf(fact.getId());
        // 沿用首次产生的会话归属，保证 MySQL conversation_id 与向量 metadata 一致，删会话时可同步清理
        String ownerConversationId = StringUtils.hasText(fact.getConversationId())
            ? fact.getConversationId() : conversationId;
        Document doc = toMemoryDocument(maskedText, type, importance, userId, ownerConversationId, factId);
        try {
            this.vectorStore.write(List.of(doc));
            markVectorSynced(fact.getId(), 1);
            log.debug("会话[{}] 记忆写入 [{}]（id={}）: {}", conversationId, type.lower(), factId, maskedText);
        } catch (Exception e) {
            // MySQL 已写入，vectorSynced 保持 0，由补偿任务重推
            log.warn("会话[{}] 记忆 {} 向量写入失败，待补偿同步", conversationId, factId, e);
        }
    }

    private @Nullable AiMemoryFact saveFact(@Nullable String existingId, String maskedText, MemoryType type,
                                            double importance, String userId, String conversationId) {
        Long factId = parseFactId(existingId);
        AiMemoryFact fact = new AiMemoryFact();
        fact.setFactType(type.lower());
        fact.setContent(maskedText);
        fact.setImportance(BigDecimal.valueOf(importance).setScale(2, RoundingMode.HALF_UP));
        fact.setHash(hashOf(userId, maskedText));
        fact.setStatus(STATUS_ACTIVE);
        fact.setValidUntil(validUntil());
        fact.setVectorSynced(0);
        try {
            if (factId != null) {
                AiMemoryFact existing = this.memoryFactService.selectAiMemoryFactById(factId);
                if (existing != null) {
                    fact.setId(factId);
                    // 保留首次产生的会话归属，避免跨会话更新后 MySQL 与向量索引归属不一致
                    fact.setConversationId(existing.getConversationId());
                    if (this.memoryFactService.updateAiMemoryFact(fact) > 0) {
                        return fact;
                    }
                }
                // 向量库残留了 MySQL 不存在的 id，回退为新增
                fact.setId(null);
            }
            fact.setUserId(userId);
            fact.setAgentId(DEFAULT_AGENT_ID);
            fact.setConversationId(conversationId);
            this.memoryFactService.insertAiMemoryFact(fact);
            return fact;
        } catch (Exception e) {
            log.warn("会话[{}] 用户事实落库失败: {}", conversationId, maskedText, e);
            return null;
        }
    }

    private void markVectorSynced(Long factId, int synced) {
        try {
            AiMemoryFact update = new AiMemoryFact();
            update.setId(factId);
            update.setVectorSynced(synced);
            this.memoryFactService.updateAiMemoryFact(update);
        } catch (Exception e) {
            log.warn("记忆 {} 更新向量同步标记失败", factId, e);
        }
    }

    private void deleteMemory(String memoryId, String conversationId) {
        if (!StringUtils.hasText(memoryId)) {
            return;
        }
        try {
            Long factId = parseFactId(memoryId);
            if (factId != null) {
                // 先软删权威存储，向量索引删除失败时由 vectorSynced=0 触发补偿
                AiMemoryFact update = new AiMemoryFact();
                update.setId(factId);
                update.setStatus(STATUS_DELETED);
                update.setVectorSynced(0);
                this.memoryFactService.updateAiMemoryFact(update);
            }
            this.vectorStore.delete(List.of(memoryId));
            log.debug("会话[{}] 记忆删除: {}", conversationId, memoryId);
        } catch (Exception e) {
            log.warn("会话[{}] 记忆 {} 删除失败，待补偿清理", conversationId, memoryId, e);
        }
    }

    private @Nullable Long parseFactId(@Nullable String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        try {
            return Long.valueOf(id.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String hashOf(String userId, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (userId + "|" + content).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private @Nullable Date validUntil() {
        return this.memoryTtlMs > 0 ? new Date(System.currentTimeMillis() + this.memoryTtlMs) : null;
    }

    private Document toMemoryDocument(String maskedText, MemoryType type, double importance,
                                      String userId, String conversationId,
                                      String documentId) {
        long now = System.currentTimeMillis();
        long expireAt = this.memoryTtlMs > 0
            ? now + this.memoryTtlMs
            : now + CHAT_MEMORY_NEVER_EXPIRE_DURATION_MS;
        Map<String, Object> metadata = new HashMap<>(16);
        metadata.put(CHAT_MEMORY_USER_ID, userId);
        metadata.put(CHAT_MEMORY_CONVERSATION_ID, conversationId);
        metadata.put(CHAT_MEMORY_TYPE, type.lower());
        metadata.put(CHAT_MEMORY_MESSAGE_TYPE, "user");
        metadata.put(CHAT_MEMORY_STATUS, STATUS_ACTIVE);
        metadata.put(CHAT_MEMORY_INGESTED_AT, now);
        metadata.put(CHAT_MEMORY_EXPIRE_AT, expireAt);
        metadata.put(CHAT_MEMORY_IMPORTANCE, importance);
        return Document.builder()
            .id(documentId)
            .text(maskedText)
            .metadata(metadata)
            .build();
    }

    /**
     * 仅做与格式无关的宽松校验：非空、不是"无"、长度上限。
     * 具体格式（第三人称、客观事实等）由 prompt 约束，避免把 prompt 契约硬编码进 Java。
     */
    private boolean isValidMemoryText(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String s = text.strip();
        return !s.startsWith("无") && s.length() <= MAX_MEMORY_TEXT_LENGTH;
    }

    private double normalizeImportance(@Nullable Double importance) {
        if (importance == null || importance < 0 || importance > 1) {
            return DEFAULT_IMPORTANCE;
        }
        return importance;
    }

    private double importanceMeta(Document doc) {
        Object value = doc.getMetadata().get(CHAT_MEMORY_IMPORTANCE);
        return value instanceof Number n ? Math.max(0, Math.min(1, n.doubleValue())) : DEFAULT_IMPORTANCE;
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) + "...(内容过长已截断)" : text;
    }

    private String maskSensitiveInfo(String text) {
        String masked = PATTERN_ID_CARD.matcher(text).replaceAll(MASK);
        masked = PATTERN_BANK_CARD.matcher(masked).replaceAll(MASK);
        masked = PATTERN_PHONE.matcher(masked).replaceAll(MASK);
        return masked;
    }

    private Filter.Expression activeMemoryFilter(FilterExpressionBuilder b, String userId, long now) {
        return b.and(
            b.eq(CHAT_MEMORY_USER_ID, userId),
            b.and(b.eq(CHAT_MEMORY_STATUS, STATUS_ACTIVE), b.gt(CHAT_MEMORY_EXPIRE_AT, now))
        ).build();
    }

    /**
     * 相似度 + 时间衰减 + 重要性 三分量融合排序，避免高重要性画像被纯相似度挤掉。
     */
    private List<Document> rankByRecency(List<Document> documents) {
        if (documents.size() <= 1) {
            return documents;
        }
        long now = System.currentTimeMillis();
        long dayMs = 24L * 60 * 60 * 1000;
        record Scored(Document doc, double score) {
        }
        List<Scored> scored = new ArrayList<>(documents.size());
        for (Document doc : documents) {
            Double rawScore = doc.getScore();
            double similarity = rawScore != null && rawScore >= 0 ? rawScore : 0.5;
            Object ingestedAt = doc.getMetadata().get(CHAT_MEMORY_INGESTED_AT);
            double ageDays = ingestedAt instanceof Number n
                ? Math.max(0, (now - n.longValue()) / (double) dayMs) : 0;
            double decay = 0.5 + 0.5 * Math.exp(-ageDays / 30.0);
            double score = 0.7 * similarity + 0.2 * decay + 0.1 * importanceMeta(doc);
            scored.add(new Scored(doc, score));
        }
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        return scored.stream().map(Scored::doc).collect(Collectors.toList());
    }

    private @Nullable String getUserId(Map<String, @Nullable Object> context) {
        Object userId = context.get(CTX_USER_ID);
        return userId != null && StringUtils.hasText(userId.toString()) ? userId.toString() : null;
    }

    private boolean isAnonymous(@Nullable String userId) {
        return !StringUtils.hasText(userId);
    }

    private int getChatMemoryTopK(Map<String, @Nullable Object> context) {
        Object fromCtx = context.get(CTX_CHAT_MEMORY_TOP_K);
        return fromCtx != null ? Integer.parseInt(fromCtx.toString()) : this.defaultTopK;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        Scheduler scheduler = this.getScheduler();
        return Mono.just(chatClientRequest)
            .publishOn(scheduler)
            .map(request -> this.before(request, streamAdvisorChain))
            .flatMapMany(streamAdvisorChain::nextStream)
            .transform(flux -> new ChatClientMessageAggregator()
                .aggregateChatClientResponse(flux, response -> this.after(response, streamAdvisorChain)));
    }

    private String escapeXml(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("&", "\u0026amp;")
            .replace("<", "\u0026lt;")
            .replace(">", "\u0026gt;")
            .replace("\"", "\u0026quot;")
            .replace("'", "\u0026apos;");
    }

    /** 提取阶段结构化输出（Phase 1），与 ExtractInstructionPrompt 的 schema 对齐 */
    public record ExtractedFact(String content, String type, Double importance) {
    }

    public record ExtractionResult(List<ExtractedFact> facts) {
    }

    /** 裁决阶段结构化输出（Phase 2），targetIndex 指向本次候选列表下标，避免 LLM 幻觉 id */
    public record Verdict(String event, Integer targetIndex, String content, String oldMemory) {
    }

    public static final class Builder {
        private Integer defaultTopK = DEFAULT_CTX_CHAT_MEMORY_TOP_K;
        private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;
        private long memoryTtlMs = DEFAULT_MEMORY_TTL_MS;
        private final VectorStore vectorStore;
        private final ChatClient chatClient;
        private final NacosPromptRegistry promptRegistry;
        private final IAiMemoryFactService memoryFactService;

        Builder(VectorStore vectorStore, ChatClient chatClient, NacosPromptRegistry promptRegistry,
                IAiMemoryFactService memoryFactService) {
            this.vectorStore = vectorStore;
            this.chatClient = chatClient;
            this.promptRegistry = promptRegistry;
            this.memoryFactService = memoryFactService;
        }

        public Builder defaultTopK(int defaultTopK) {
            Assert.isTrue(defaultTopK > 0, "topK must be greater than 0");
            this.defaultTopK = defaultTopK;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            Assert.notNull(scheduler, "scheduler cannot be null");
            this.scheduler = scheduler;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder memoryTtlMs(long memoryTtlMs) {
            Assert.isTrue(memoryTtlMs >= 0, "memoryTtlMs must be >= 0");
            this.memoryTtlMs = memoryTtlMs;
            return this;
        }

        public VectorStoreChatMemoryAdvisor build() {
            return new VectorStoreChatMemoryAdvisor(this.defaultTopK,
                this.order, this.scheduler, this.vectorStore, this.chatClient,
                this.memoryTtlMs, promptRegistry, memoryFactService);
        }
    }
}