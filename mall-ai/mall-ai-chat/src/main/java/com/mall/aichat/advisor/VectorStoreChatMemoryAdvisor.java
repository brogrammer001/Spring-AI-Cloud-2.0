package com.mall.aichat.advisor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.aichat.config.NacosPromptRegistry;
import com.mall.aichat.domain.ExtractedMemory;
import com.mall.aichat.domain.MemoryOperation;
import com.mall.aichat.domain.MemoryType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mall.aichat.constant.ChatConstants.*;

public class VectorStoreChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private final Logger log = LoggerFactory.getLogger(VectorStoreChatMemoryAdvisor.class);

    private static final int DEFAULT_CTX_CHAT_MEMORY_TOP_K = 5;
    private static final long DEFAULT_MEMORY_TTL_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long NEVER_EXPIRE_EXPIRE_AT_MS = 100L * 365 * 24 * 60 * 60 * 1000;
    private static final int MAX_EXTRACT_INPUT_LENGTH = 400;
    private static final int DECISION_CANDIDATE_TOP_K = 3;
    private static final double DECISION_SIMILARITY_THRESHOLD = 0.75;
    private static final int MAX_MEMORY_TEXT_LENGTH = 100;
    private static final int MAX_MEMORIES_PER_TURN = 5;
    private static final String MEMORY_TEXT_PREFIX = "用户";
    private static final int MAX_ASSISTANT_TEXT_LENGTH = 300;
    private static final String CTX_CURRENT_USER_TEXT = "_vector_memory_current_user_text";

    private static final Pattern PATTERN_PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern PATTERN_ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");
    private static final Pattern PATTERN_BANK_CARD = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");
    private static final String MASK = "***";

    private final int defaultTopK;
    private final int order;
    private final Scheduler scheduler;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final long memoryTtlMs;
    private final NacosPromptRegistry promptRegistry;

    private VectorStoreChatMemoryAdvisor(int defaultTopK,
                                       int order, Scheduler scheduler, VectorStore vectorStore,
                                       ChatClient chatClient, ObjectMapper objectMapper, long memoryTtlMs, NacosPromptRegistry promptRegistry) {
        Assert.isTrue(defaultTopK > 0, "topK must be greater than 0");
        Assert.notNull(scheduler, "scheduler cannot be null");
        Assert.notNull(vectorStore, "vectorStore cannot be null");
        Assert.notNull(chatClient, "chatClient cannot be null");
        Assert.notNull(objectMapper, "objectMapper cannot be null");
        Assert.notNull(promptRegistry, "promptRegistry cannot be null");
        Assert.isTrue(memoryTtlMs >= 0, "memoryTtlMs must be >= 0");
        this.defaultTopK = defaultTopK;
        this.order = order;
        this.scheduler = scheduler;
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.memoryTtlMs = memoryTtlMs;
        this.promptRegistry = promptRegistry;
    }

    public static Builder builder(VectorStore vectorStore, ChatClient chatClient, NacosPromptRegistry promptRegistry) {
        return new Builder(vectorStore, chatClient, promptRegistry);
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

        storeMemoryAsync(userText, assistantText, userId, conversationId);
        return chatClientResponse;
    }

    private void storeMemoryAsync(String userText, @Nullable String assistantText,
                                  String userId, String conversationId) {
        Mono.fromRunnable(() -> storeMemory(userText, assistantText, userId, conversationId))
            .subscribeOn(this.scheduler)
            .subscribe(_ -> {
            }, e -> log.error("会话[{}] 记忆异步存储失败", conversationId, e));
    }

    private void storeMemory(String userText, @Nullable String assistantText,
                             String userId, String conversationId) {
        if (!StringUtils.hasText(userText)) {
            return;
        }
        try {
            List<ExtractedMemory> extracted = extractMemories(userText, assistantText, conversationId);
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
        }
    }

    private List<ExtractedMemory> extractMemories(String userText, @Nullable String assistantText,
                                                  String conversationId) {
        StringBuilder input = new StringBuilder();
        input.append("【用户消息】").append(truncate(userText, MAX_EXTRACT_INPUT_LENGTH));
        if (StringUtils.hasText(assistantText)) {
            input.append(System.lineSeparator())
                .append("【AI回复】")
                .append(truncate(assistantText, MAX_ASSISTANT_TEXT_LENGTH));
        }

        String output;
        try {
            output = this.chatClient.prompt()
                .system(promptRegistry.get("ExtractInstructionPrompt"))
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
            String json = sanitizeJsonOutput(output);
            List<Map<String, Object>> rawList = this.objectMapper.readValue(
                json, new TypeReference<List<Map<String, Object>>>() {
                });
            if (rawList == null || rawList.isEmpty()) {
                return List.of();
            }
            List<ExtractedMemory> result = new ArrayList<>();
            int limit = Math.min(rawList.size(), MAX_MEMORIES_PER_TURN);
            for (Map<String, Object> raw : rawList.subList(0, limit)) {
                String content = String.valueOf(raw.getOrDefault("content", "")).strip();
                String typeStr = String.valueOf(raw.getOrDefault("type", MemoryType.FACT.lower()));
                if (!isValidMemoryText(content)) {
                    log.debug("会话[{}] 提取的记忆不合规，跳过: {}", conversationId, content);
                    continue;
                }
                result.add(new ExtractedMemory(content, MemoryType.fromLower(typeStr)));
            }
            return result;
        } catch (Exception e) {
            log.warn("会话[{}] 提取输出 JSON 解析失败，本轮不写记忆: {}", conversationId, output, e);
            return List.of();
        }
    }

    private void decideAndApply(ExtractedMemory memory,
                                String userId, String conversationId) {
        List<Document> candidates = searchDecisionCandidates(memory.content(), userId, conversationId);

        if (candidates.isEmpty()) {
            writeMemory(memory.content(), memory.type(), userId, conversationId);
            return;
        }

        for (Document candidate : candidates) {
            if (candidate.getText().strip().equals(memory.content())) {
                writeMemory(memory.content(), memory.type(), userId, conversationId);
                try {
                    this.vectorStore.delete(List.of(candidate.getId()));
                    log.debug("会话[{}] 记忆完全相同，续期: {}", conversationId, memory.content());
                } catch (Exception delEx) {
                    log.warn("会话[{}] 旧记忆 {} 删除失败，暂与新结果共存",
                        conversationId, candidate.getId(), delEx);
                }
                return;
            }
        }

        MemoryDecision decision = decide(memory, candidates, conversationId);
        if (decision == null) {
            writeMemory(memory.content(), memory.type(), userId, conversationId);
            return;
        }

        switch (decision.op()) {
            case ADD -> writeMemory(memory.content(), memory.type(), userId, conversationId);
            case UPDATE -> {
                writeMemory(decision.content(), memory.type(), userId, conversationId);
                deleteMemory(decision.targetId(), conversationId);
            }
            case DELETE -> deleteMemory(decision.targetId(), conversationId);
            case NOOP -> log.debug("会话[{}] 记忆重复，跳过: {}", conversationId, memory.content());
        }
    }

    private List<Document> searchDecisionCandidates(String memoryText, String userId, String conversationId) {
        Filter.Expression expression = activeMemoryFilter(new FilterExpressionBuilder(),
            userId, System.currentTimeMillis());
        try {
            return this.vectorStore.similaritySearch(SearchRequest.builder()
                .query(memoryText)
                .topK(DECISION_CANDIDATE_TOP_K)
                .similarityThreshold(DECISION_SIMILARITY_THRESHOLD)
                .filterExpression(expression)
                .build());
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
            for (Document doc : candidates) {
                Object typeMeta = doc.getMetadata().get(CHAT_MEMORY_TYPE);
                String type = typeMeta != null ? typeMeta.toString() : MemoryType.FACT.lower();
                candidateText.append("  - id=").append(doc.getId())
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

            return parseDecision(output, memory, conversationId);
        } catch (Exception e) {
            log.warn("会话[{}] 决策 LLM 调用失败，按新增写入", conversationId, e);
            return null;
        }
    }

    private @Nullable MemoryDecision parseDecision(@Nullable String output, ExtractedMemory fallback,
                                                   String conversationId) {
        if (!StringUtils.hasText(output)) {
            return null;
        }
        try {
            String json = sanitizeJsonOutput(output);
            Map<String, Object> map = this.objectMapper.readValue(
                json, new TypeReference<Map<String, Object>>() {
                });
            String opStr = String.valueOf(map.getOrDefault("op", "")).strip().toUpperCase();
            String targetId = String.valueOf(map.getOrDefault("target_id", "")).strip();
            String content = String.valueOf(map.getOrDefault("content", fallback.content())).strip();

            MemoryOperation op;
            try {
                op = MemoryOperation.valueOf(opStr);
            } catch (IllegalArgumentException e) {
                log.warn("会话[{}] 决策输出 op 非法: {}，按新增处理", conversationId, opStr);
                return null;
            }
            if ((op == MemoryOperation.UPDATE || op == MemoryOperation.DELETE)
                && !StringUtils.hasText(targetId)) {
                log.warn("会话[{}] UPDATE/DELETE 缺少 targetId，按新增处理", conversationId);
                return new MemoryDecision(MemoryOperation.ADD, "", content);
            }
            if (op == MemoryOperation.UPDATE && !isValidMemoryText(content)) {
                log.warn("会话[{}] UPDATE 输出不合规，按新增处理", conversationId);
                return null;
            }
            return new MemoryDecision(op, targetId, content);
        } catch (Exception e) {
            log.warn("会话[{}] 决策输出 JSON 解析失败，按新增处理: {}", conversationId, output, e);
            return null;
        }
    }

    private void writeMemory(String text, MemoryType type,
                             String userId, String conversationId) {
        Document doc = toMemoryDocument(text, type, userId, conversationId);
        this.vectorStore.write(List.of(doc));
        log.debug("会话[{}] 记忆写入 [{}]: {}", conversationId, type.lower(), text);
    }

    private void deleteMemory(String memoryId, String conversationId) {
        if (!StringUtils.hasText(memoryId)) {
            return;
        }
        try {
            this.vectorStore.delete(List.of(memoryId));
            log.debug("会话[{}] 记忆删除: {}", conversationId, memoryId);
        } catch (Exception e) {
            log.warn("会话[{}] 记忆 {} 删除失败，暂留待下轮去重", conversationId, memoryId, e);
        }
    }

    private Document toMemoryDocument(String text, MemoryType type,
                                      String userId, String conversationId) {
        long now = System.currentTimeMillis();
        long expireAt = this.memoryTtlMs > 0
            ? now + this.memoryTtlMs
            : now + NEVER_EXPIRE_EXPIRE_AT_MS;
        Map<String, Object> metadata = new HashMap<>(16);
        metadata.put(CHAT_MEMORY_USER_ID, userId);
        metadata.put(CHAT_MEMORY_CONVERSATION_ID, conversationId);
        metadata.put(CHAT_MEMORY_TYPE, type.lower());
        metadata.put(CHAT_MEMORY_MESSAGE_TYPE, "user");
        metadata.put(CHAT_MEMORY_STATUS, STATUS_ACTIVE);
        metadata.put(CHAT_MEMORY_INGESTED_AT, now);
        metadata.put(CHAT_MEMORY_EXPIRE_AT, expireAt);
        return Document.builder().text(maskSensitiveInfo(text)).metadata(metadata).build();
    }

    private boolean isValidMemoryText(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String s = stripPrefixAndQuotes(text);
        if (s.startsWith("无")) {
            return false;
        }
        return s.startsWith(MEMORY_TEXT_PREFIX) && s.length() <= MAX_MEMORY_TEXT_LENGTH;
    }

    private String stripPrefixAndQuotes(String text) {
        String s = text.strip();
        s = s.replaceFirst("^(输出|总结|合并后|记忆)[:：]\\s*", "");
        s = s.replaceAll("^[\"'“”「『]|[\"'“”」』]$", "").strip();
        return s;
    }

    private String stripCodeFence(String output) {
        String s = output.strip();
        s = s.replaceAll("^```(json)?\\s*", "").replaceAll("```$", "").strip();
        return s;
    }

    private String sanitizeJsonOutput(String output) {
        String s = stripCodeFence(output);
        if (!StringUtils.hasText(s)) {
            return s;
        }
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 2) {
            try {
                String unquoted = this.objectMapper.readValue(s, String.class);
                if (StringUtils.hasText(unquoted)) {
                    s = unquoted.strip();
                }
            } catch (Exception ignored) {
            }
        }
        if (s.contains("\\\"")) {
            s = s.replace("\\\"", "\"");
        }
        if (s.contains("\\'")) {
            s = s.replace("\\'", "'");
        }
        s = stripCodeFence(s);
        return s;
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

    private List<Document> rankByRecency(List<Document> documents) {
        if (documents.size() <= 1) {
            return documents;
        }
        long now = System.currentTimeMillis();
        long dayMs = 24L * 60 * 60 * 1000;
        record Scored(Document doc, double score) {
        }
        List<Scored> scored = new ArrayList<>(documents.size());
        boolean hasScore = false;
        for (Document doc : documents) {
            double base = doc.getScore() != null ? doc.getScore() : -1;
            if (base >= 0) {
                hasScore = true;
            }
            Object ingestedAt = doc.getMetadata().get(CHAT_MEMORY_INGESTED_AT);
            double ageDays = ingestedAt instanceof Number n
                ? Math.max(0, (now - n.longValue()) / (double) dayMs) : 0;
            double decay = 0.5 + 0.5 * Math.exp(-ageDays / 30.0);
            scored.add(new Scored(doc, (base >= 0 ? base : 0.5) * decay));
        }
        List<Scored> sorted = new ArrayList<>(scored);
        if (hasScore) {
            sorted.sort((a, b2) -> Double.compare(b2.score(), a.score()));
        } else {
            sorted.sort((a, b2) -> {
                Object ta = a.doc().getMetadata().get(CHAT_MEMORY_INGESTED_AT);
                Object tb = b2.doc().getMetadata().get(CHAT_MEMORY_INGESTED_AT);
                long la = ta instanceof Number n ? n.longValue() : 0;
                long lb = tb instanceof Number n ? n.longValue() : 0;
                return Long.compare(lb, la);
            });
        }
        return sorted.stream().map(Scored::doc).collect(Collectors.toList());
    }

    private String getUserId(Map<String, @Nullable Object> context) {
        Object userId = context.get(CTX_USER_ID);
        return userId != null ? userId.toString() : "anonymous";
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

    public static final class Builder {
        private Integer defaultTopK = DEFAULT_CTX_CHAT_MEMORY_TOP_K;
        private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;
        private long memoryTtlMs = DEFAULT_MEMORY_TTL_MS;
        private ObjectMapper objectMapper = new ObjectMapper();
        private final VectorStore vectorStore;
        private final ChatClient chatClient;
        private final NacosPromptRegistry promptRegistry;

        Builder(VectorStore vectorStore, ChatClient chatClient,  NacosPromptRegistry promptRegistry) {
            this.vectorStore = vectorStore;
            this.chatClient = chatClient;
            this.promptRegistry = promptRegistry;
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

        public Builder objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "objectMapper cannot be null");
            this.objectMapper = objectMapper;
            return this;
        }

        public VectorStoreChatMemoryAdvisor build() {
            return new VectorStoreChatMemoryAdvisor(this.defaultTopK,
                this.order, this.scheduler, this.vectorStore, this.chatClient,
                this.objectMapper, this.memoryTtlMs, promptRegistry);
        }
    }
}