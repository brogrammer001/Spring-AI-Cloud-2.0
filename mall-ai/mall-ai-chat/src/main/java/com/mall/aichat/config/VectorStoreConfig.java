package com.mall.aichat.config;

import io.weaviate.client.WeaviateClient;
import io.weaviate.client.v1.misc.model.VectorIndexConfig;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.weaviate.WeaviateVectorStore;
import org.springframework.ai.vectorstore.weaviate.WeaviateVectorStoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.List;

/**
 * 向量库配置
 * <p>三库分职责：① 会话记忆库 ② 知识库 ③ 工具索引库，启动时统一初始化 Schema。</p>
 * <p>Schema 设计约定：</p>
 * <ul>
 *   <li>过滤字段必须平铺到顶层（{@code meta_} 前缀），并按 TEXT/NUMBER 声明正确类型，
 *       且与 {@code filterMetadataFields} 一一对应，否则过滤表达式无法命中；</li>
 *   <li>{@code metadata} JSON 字段仅存放"低频读、不参与过滤"的扩展信息；</li>
 *   <li>预留字段（如记忆 Schema 的 memoryType/status/expireAt 等）提前声明，
 *       避免后续新增 filterable 字段时需要全量重灌数据；</li>
 *   <li>HNSW 参数已针对 1536 维 embedding 显式调优；参数调整仅对新建 Class 生效，
 *       存量 Class 需走「新建 v2 + 重灌迁移」流程；</li>
 *   <li>数值型过滤字段统一使用 Weaviate {@code number} 类型，与 Spring AI NUMBER 的
 *       序列化及过滤转换（valueNumber 浮点语义）对齐，避免 {@code int} 字段拒绝浮点值；</li>
 *   <li>存量数据的预留字段均为 null，不会命中新字段的过滤条件，写入方上线时需安排一次批量回填。</li>
 * </ul>
 * <p>初始化时序：Schema 初始化器为普通 Bean，三个 VectorStore Bean 均 {@code @DependsOn} 它，
 * 确保完整 Schema 先于首次写入落地——否则 Weaviate 服务端 implicit auto-schema 会在首条数据写入时
 * 按推断自动建类（仅 content/metadata 两个 text 字段），抢占后过滤字段将永远缺失，故障推迟到运行期。</p>
 * <p>版本前置条件：Weaviate 服务端 ≥ 1.24（indexFilterable/indexSearchable 需 ≥ 1.23，
 * tokenization 完整支持需 ≥ 1.24）；weaviate-client-java 5.2.0 已验证支持所用 Property builder 方法。</p>
 */
@Configuration
@ConditionalOnProperty(name = "vectorstore.enabled", havingValue = "true", matchIfMissing = true)
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Value("${vectorstore.weaviate.knowledge-object-class}")
    private String weaviateKnowledgeObjectClass;

    @Value("${vectorstore.weaviate.chat-memory-object-class}")
    private String weaviateChatMemoryObjectClass;

    @Value("${vectorstore.weaviate.tool-index-object-class}")
    private String weaviateToolIndexObjectClass;

    /**
     * ① 会话记忆向量库 (用于 VectorStoreChatMemoryAdvisor 检索对话历史 + 压缩记忆)
     * <p>过滤字段与读写方对齐：{@code conversationId}（advisor 检索/压缩服务删除）、
     * {@code messageType}（advisor 读写）；其余为记忆 Schema 预留字段</p>
     */
    @Bean("conversationVectorStore")
    @DependsOn("weaviateSchemaInitializer") // 完整 Schema 必须先于首次写入落地，避免被 Weaviate 隐式 auto-schema 抢占
    public VectorStore conversationVectorStore(WeaviateClient weaviateClient, OpenAiEmbeddingModel openAiEmbedding) {
        return buildStore(weaviateClient, openAiEmbedding, weaviateChatMemoryObjectClass, List.of(
            WeaviateVectorStore.MetadataField.text("conversationId"),
            WeaviateVectorStore.MetadataField.text("messageType"),
            // ↓ 记忆 Schema 预留字段（写入方落地后即可直接过滤，无需变更 Schema）
            WeaviateVectorStore.MetadataField.text("userId"),
            WeaviateVectorStore.MetadataField.text("memoryType"),
            WeaviateVectorStore.MetadataField.text("category"),
            WeaviateVectorStore.MetadataField.text("status"),
            WeaviateVectorStore.MetadataField.text("supersededBy"),
            WeaviateVectorStore.MetadataField.text("entities"),
            WeaviateVectorStore.MetadataField.number("validFrom"),
            WeaviateVectorStore.MetadataField.number("ingestedAt"),
            WeaviateVectorStore.MetadataField.number("expireAt"),
            WeaviateVectorStore.MetadataField.number("hitCount"),
            WeaviateVectorStore.MetadataField.number("mergeCount")
        ));
    }

    /**
     * ② 知识库向量库 (用于 RAG 检索文档知识)
     * <p>过滤字段与读写方对齐：{@code knowledgeId}（RAG 检索/删除）、
     * {@code source}/{@code filename}（写入方已携带）；其余为预留字段</p>
     */
    @Bean("knowledgeVectorStore")
    @DependsOn("weaviateSchemaInitializer") // 完整 Schema 必须先于首次写入落地，避免被 Weaviate 隐式 auto-schema 抢占
    public VectorStore knowledgeVectorStore(WeaviateClient weaviateClient, OpenAiEmbeddingModel embeddingModel) {
        return buildStore(weaviateClient, embeddingModel, weaviateKnowledgeObjectClass, List.of(
            WeaviateVectorStore.MetadataField.text("knowledgeId"),
            WeaviateVectorStore.MetadataField.text("source"),
            WeaviateVectorStore.MetadataField.text("filename"),
            // ↓ 预留字段
            WeaviateVectorStore.MetadataField.text("docType"),
            WeaviateVectorStore.MetadataField.number("chunkIndex"),
            WeaviateVectorStore.MetadataField.number("version"),
            WeaviateVectorStore.MetadataField.number("updatedAt")
        ));
    }

    /**
     * ③ 工具索引向量库 (用于 ToolSearch 检索工具定义)
     * <p>{@code sessionId} 为 Spring AI {@code VectorToolIndex} 框架级字段：
     * 索引时写入、检索时按会话过滤，不可删除；{@code toolName} 同步声明便于按名过滤</p>
     * <p>语义提醒：按 sessionId 过滤只能发现本会话注册的工具。TODO 未来若引入「全局工具 +
     * 会话级工具」混合发现，需提前约定写入契约：全局工具 sessionId 写空串 ""，
     * 检索时用 OR 表达式放行：{@code sessionId == '' OR sessionId == 当前会话ID}</p>
     */
    @Bean("toolVectorStore")
    @DependsOn("weaviateSchemaInitializer") // 完整 Schema 必须先于首次写入落地，避免被 Weaviate 隐式 auto-schema 抢占
    public VectorStore toolVectorStore(WeaviateClient weaviateClient, OpenAiEmbeddingModel embeddingModel) {
        return buildStore(weaviateClient, embeddingModel, weaviateToolIndexObjectClass, List.of(
            WeaviateVectorStore.MetadataField.text("sessionId"),
            WeaviateVectorStore.MetadataField.text("toolName")
        ));
    }

    /**
     * 构建 VectorStore：options 指定 Class，filterMetadataFields 声明可过滤字段及类型
     */
    private VectorStore buildStore(WeaviateClient weaviateClient, OpenAiEmbeddingModel embeddingModel,
                                   String objectClass, List<WeaviateVectorStore.MetadataField> filterFields) {
        return WeaviateVectorStore.builder(weaviateClient, embeddingModel)
            .options(optionsFor(objectClass))
            .filterMetadataFields(filterFields)
            .build();
    }

    /**
     * WeaviateVectorStoreOptions 单一来源：VectorStore Bean 与 Schema 初始化均从此获取，
     * 保证 contentFieldName/metaFieldPrefix 在读写侧与 Schema 声明侧永远一致，
     * 避免未来自定义字段名时两侧静默分叉（写入字段 A、Schema 声明字段 B）
     */
    private WeaviateVectorStoreOptions optionsFor(String objectClass) {
        WeaviateVectorStoreOptions weaviateOptions = new WeaviateVectorStoreOptions();
        weaviateOptions.setObjectClass(objectClass);
        return weaviateOptions;
    }

    /**
     * Schema 初始化器（仅创建缺失的 Class，已存在则跳过；失败直接终止启动）。
     * <p>必须是普通 Bean（而非 ApplicationRunner）并由三个 VectorStore Bean {@code @DependsOn}：
     * 容器刷新阶段即完成建类，赶在任何业务写入触发 Weaviate 服务端隐式 auto-schema 之前，
     * 否则自动建出的 Class 只有 content/metadata 两个 text 字段，过滤字段全部缺失</p>
     */
    @Bean("weaviateSchemaInitializer")
    public Boolean weaviateSchemaInitializer(WeaviateClient weaviateClient) {
        // prefix/content 字段与 VectorStore Bean 的 options 同源，避免 Schema 与读写侧静默分叉
        WeaviateVectorStoreOptions weaviateOptions = optionsFor(weaviateChatMemoryObjectClass);
        String prefix = weaviateOptions.getMetaFieldPrefix();
        String contentField = weaviateOptions.getContentFieldName();

        createSchemaIfNotExists(weaviateClient, weaviateChatMemoryObjectClass,
            chatMemoryProperties(prefix, contentField), "会话记忆向量库");
        createSchemaIfNotExists(weaviateClient, weaviateKnowledgeObjectClass,
            knowledgeProperties(prefix, contentField), "知识库向量库");
        createSchemaIfNotExists(weaviateClient, weaviateToolIndexObjectClass,
            toolIndexProperties(prefix, contentField), "工具索引向量库");
        return Boolean.TRUE;
    }

    /**
     * 会话记忆库属性：Spring AI 固定结构 + 平铺的过滤字段（类型与 MetadataField 一一对应）
     */
    private List<Property> chatMemoryProperties(String prefix, String contentField) {
        return List.of(
            // Spring AI 固定结构
            Property.builder().name("metadata").dataType(List.of("text")).build(),
            Property.builder().name(contentField).dataType(List.of("text"))
                .indexSearchable(true).build(),
            // 现有读写方使用的过滤字段
            Property.builder().name(prefix + "conversationId").dataType(List.of("text"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "messageType").dataType(List.of("text"))
                .indexFilterable(true).build(),
            // ↓ 记忆 Schema 预留字段（写入方落地后即可过滤，无需再改 Schema）
            // TODO 存量数据预留字段均为 null，不会命中这些过滤；记忆写入方上线时需批量回填历史数据（如 userId/status 默认值）
            Property.builder().name(prefix + "userId").dataType(List.of("text"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "memoryType").dataType(List.of("text"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "category").dataType(List.of("text"))
                .indexFilterable(true).build(),
            // 状态枚举用字符串而非 boolean（Weaviate boolean 过滤存在已知问题）
            Property.builder().name(prefix + "status").dataType(List.of("text"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "supersededBy").dataType(List.of("text"))
                .indexFilterable(true).build(),
            // 实体列表：整体作为单个 token，避免空格切词打散实体
            Property.builder().name(prefix + "entities").dataType(List.of("text"))
                .indexFilterable(true).indexSearchable(true)
                .tokenization("field").build(),
            // 数值型：时间戳
            Property.builder().name(prefix + "validFrom").dataType(List.of("number"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "ingestedAt").dataType(List.of("number"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "expireAt").dataType(List.of("number"))
                .indexFilterable(true).build(),
            // 数值型：计数（统一 number，与 Spring AI NUMBER 序列化对齐，计数器语义无损）
            Property.builder().name(prefix + "hitCount").dataType(List.of("number"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "mergeCount").dataType(List.of("number"))
                .indexFilterable(true).build()
        );
    }

    /**
     * 知识库属性：字段与 knowledgeVectorStore 的 filterMetadataFields 一一对应，便于就近对照
     */
    private List<Property> knowledgeProperties(String prefix, String contentField) {
        return List.of(
            // Spring AI 固定结构
            Property.builder().name("metadata").dataType(List.of("text")).build(),
            Property.builder().name(contentField).dataType(List.of("text"))
                .indexSearchable(true).build(),
            // 现有读写方使用的过滤字段（RAG 检索/删除、写入方已携带）
            Property.builder().name(prefix + "knowledgeId").dataType(List.of("text"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "source").dataType(List.of("text"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "filename").dataType(List.of("text"))
                .indexFilterable(true).build(),
            // ↓ 预留字段
            Property.builder().name(prefix + "docType").dataType(List.of("text"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "chunkIndex").dataType(List.of("number"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "version").dataType(List.of("number"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "updatedAt").dataType(List.of("number"))
                .indexFilterable(true).build()
        );
    }

    /**
     * 工具索引库属性：字段与 VectorToolIndex 框架写入的 metadata 对齐，
     * 与 toolVectorStore 的 filterMetadataFields 一一对应，便于就近对照
     */
    private List<Property> toolIndexProperties(String prefix, String contentField) {
        return List.of(
            // Spring AI 固定结构
            Property.builder().name("metadata").dataType(List.of("text")).build(),
            Property.builder().name(contentField).dataType(List.of("text"))
                .indexSearchable(true).build(),
            // 框架级字段：索引时写入、检索时按会话过滤，不可删除（契约见 toolVectorStore 注释）
            Property.builder().name(prefix + "sessionId").dataType(List.of("text"))
                .indexFilterable(true).build(),
            Property.builder().name(prefix + "toolName").dataType(List.of("text"))
                .indexFilterable(true).build()
        );
    }

    /**
     * 辅助方法：检查并创建 Schema（fail-fast：失败直接抛异常终止启动）。
     * <p>Schema 是三个向量库读写/过滤的前置条件，创建失败（Weaviate 不可达、权限问题等）
     * 若吞掉异常继续启动，所有读写故障将被推迟到运行期且难以归因，故选择启动期快速失败</p>
     * <p>HNSW 参数针对 1536 维 embedding 调优：
     * efConstruction=200 提高索引质量（导入稍慢），maxConnections=48 平衡召回与查询延迟，
     * 显式限定动态 ef 区间保证百万级以下查询稳定性</p>
     */
    private void createSchemaIfNotExists(WeaviateClient weaviateClient, String className,
                                         List<Property> properties, String description) {
        try {
            Boolean exists = weaviateClient.schema().exists().withClassName(className).run().getResult();
            if (Boolean.TRUE.equals(exists)) {
                // 正常路径：Schema 已存在，仅记录 info，避免触发误告警
                log.info("Schema [{}] 已存在，跳过创建。", className);
                return;
            }

            VectorIndexConfig vectorIndexConfig = VectorIndexConfig.builder()
                .distance("cosine")
                .efConstruction(200)
                .maxConnections(48)
                .dynamicEfMin(100)
                .dynamicEfMax(500)
                .build();

            WeaviateClass weaviateClass = WeaviateClass.builder()
                .className(className)
                .description(description)
                .vectorizer("none") // 向量由 Spring AI 计算后传入
                .vectorIndexType("hnsw")
                .vectorIndexConfig(vectorIndexConfig)
                .properties(properties)
                .build();

            weaviateClient.schema().classCreator().withClass(weaviateClass).run();
            log.info("Schema [{}] 创建成功。", className);
        } catch (Exception e) {
            log.error("初始化 Schema [{}] 失败，应用终止启动", className, e);
            throw new IllegalStateException("Weaviate Schema 初始化失败: " + className, e);
        }
    }
}
