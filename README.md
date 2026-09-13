# Spring AI 2.0 企业级 ChatBot 架构与实现指南

## 1. 核心架构概览

本系统基于 **Spring Boot 4.0.6 + Spring Cloud 2025.1.1 + Spring AI 2.0 + Spring AI Alibaba 2.0 + Java 25** 微服务构建，采用 **"三层存储 + 双模记忆 + 工具编排"** 架构。

### 1.1 模块划分

| 模块 | 端口 | 职责 |
| :--- | :--- | :--- |
| `mall-ai-chat` | 9994 | **Agent 编排服务**：调用 LLM、管理记忆、RAG 检索、SSE 流式推送、工具调用拦截 |
| `mall-ai-mcp-server` | 9995 | **MCP 工具服务**：提供原子工具能力（CRUD、NL2SQL、业务聚合），通过 MCP 协议暴露 |
| `mall-ai-mcp-gateway` | 9999 | **MCP 网关服务**：基于 Spring AI Alibaba MCP Gateway，通过 Nacos 聚合多个 MCP Server 的工具，统一对外暴露 `/mcp` 端点 |

### 1.2 技术栈

| 层面 | 技术选型 |
| :--- | :--- |
| JDK | Java 25 |
| 框架 | Spring Boot 4.0.6 + Spring Cloud 2025.1.1 |
| AI 框架 | Spring AI 2.0.0 + Spring AI Alibaba 2.0.0-M1.1 |
| AI Alibaba 能力 | DashScope（通义千问接入）+ MCP Gateway（工具聚合网关）+ MCP Registry（服务注册发现） |
| LLM | 通义千问 qwen3.7-flash（OpenAI 兼容协议，经 Spring AI Alibaba DashScope 接入） |
| Embedding | Qwen3-Embedding-4B（本地部署，端口 8889） |
| 向量数据库 | Weaviate（端口 18080） |
| 热缓存 | Redis |
| 持久化 | MySQL |
| 注册中心 | Nacos |
| 文档解析 | Apache Tika + POI + FastExcel（本地解析 PDF/Word/Excel/CSV/音频元数据） |
| 图片 OCR | 本地 MinerU-OCR 视觉模型（opendatalab/MinerU2.5-Pro-2605-1.2B，端口 8890） |
| 重排序 | Reranker 模型（本地部署，端口 8887） |
| 工具协议 | MCP (Model Context Protocol) + Streamable HTTP |

### 1.3 核心架构图

```
┌─────────────────────────────────────────────────────────┐
│                    mall-ai-chat (9994)                    │
│                   Agent 编排服务                           │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │ChatAgent │→ │ChatClient│→ │Advisor链 │→ │  LLM    │ │
│  │ (SSE流式) │  │ (Builder) │  │ (7层拦截) │  │(Qwen3.7)│ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
│       │              │             │                     │
│       │     ┌────────┴───────┐    │                     │
│       │     │  RAG 检索引擎   │    │ 工具调用拦截         │
│       │     │ VectorStore    │    │ ReturnDirectWrapper  │
│       │     │ + Reranker     │    │ + ToolDataCache      │
│       │     └────────────────┘    └──────────┬──────────┘
│       │                                      │
│  ┌────┴────┐                          MCP   │
│  │记忆系统  │                    ┌──────────┴──────────┐
│  │Session+MySQL│                 │  MCP Client (Async)  │
│  │+VectorStore│                   └──────────┬──────────┘
│  └─────────┘                                │ HTTP
└──────────────────────────────────────────────┼───────────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          │                          │
                    ▼                          ▼                          │
┌──────────────────────────────────┐  ┌─────────────────────┐            │
│   mall-ai-mcp-gateway (9999)     │  │  mcp-echarts         │            │
│   MCP 网关 (AI Alibaba Gateway)   │  │  (外部 ModelScope)   │            │
│                                  │  └─────────────────────┘            │
│  ┌────────────────────────────┐  │                                     │
│  │ 聚合多个 MCP Server 工具    │  │                                     │
│  │ spring-ai-alibaba-         │  │                                     │
│  │ starter-mcp-gateway        │  │                                     │
│  └────────────┬───────────────┘  │                                     │
│               │ Nacos 服务发现    │                                     │
└───────────────┼──────────────────┘                                     │
                │                                                        │
                ▼                                                        │
┌────────────────────────────────────────────────────────────────────────┘
│              mall-ai-mcp-server (9995)
│                MCP 工具服务
│                                    ┌─────────────────────┐
│                                    │  MCP Server         │
│                                    │  (Streamable HTTP)  │
│                                    └─────────┬───────────┘
│                          ┌───────────────────┼─────────┐
│                          │                   │         │
│                   ┌──────┴──────┐    ┌───────┴──────┐
│                   │BaseToolSvc  │    │ Nl2SqlTool   │
│                   │ (抽象基类)   │    │ (SQL生成执行) │
│                   └──────┬──────┘    └──────────────┘
│          ┌───────┬───────┼───────┬─────────┐
│     ┌────┴──┐┌───┴───┐┌──┴───┐┌─┴──────┐┌─┴────────┐
│     │DeptTool││UserTool││RoleTool││NoticePost││DeptBizTool│
│     │        ││        ││        ││          ││(聚合工具) │
│     └────────┘└────────┘└────────┘└──────────┘└──────────┘
│                          │
│                   ┌──────┴──────┐
│                   │RemoteXxxSvc │ → Feign → mall-system
│                   │(Feign调用)   │
│                   └─────────────┘
└────────────────────────────────────────────────────────────
```

**工具调用链路**：
```
LLM 决定调用工具
  → mall-ai-chat MCP Client (9994)
    ├─→ mall-ai-mcp-gateway (9999)        # 内部网关
    │     └─ Nacos 发现 → mall-ai-mcp-server (9995)  # 实际工具执行
    └─→ mcp-echarts (ModelScope 外部 MCP) # 图表生成
```

---

## 2. Agent 编排服务 (mall-ai-chat)

### 2.1 目录结构

```
com.mall.aichat
├── MallAiChatApplication.java          # 启动类
├── agent/
│   └── ChatAgent.java                  # 聊天入口（SSE 流式，支持 userId / tenantId / deptId / traceId）
├── config/
│   ├── ChatClientConfig.java           # ChatClient Bean 配置（核心，装配 7 层 Advisor 链 + smallChatClient 概述小模型）
│   ├── VectorStoreConfig.java          # 三个 VectorStore Bean（字段常量收敛到 ChatConstants）
│   ├── AgentEventSinkManager.java      # SSE 旁路推送管理（tool_call / rag_retrieve 事件）
│   └── SaLlmConfig.java                # MinerU RestClient + 异步线程池（窗口记忆改由 SessionMemoryAdvisor 承载）
├── constant/
│   └── ChatConstants.java              # 向量库字段常量 + Advisor context key 统一契约
├── extractor/                           # 文档内容提取器（策略模式）
│   ├── Extractor.java                  # 提取器接口（supports + extract）
│   ├── ExtractorFactory.java           # 工厂类（Spring Bean 自动发现，按文件后缀路由）
│   ├── AbstractTikaExtractor.java      # 基于 Apache Tika 的抽象基类（结构化 Markdown + 纯文本 + 输出上限降级）
│   ├── PdfExtractor.java               # PDF 提取器（Tika 结构化 Markdown，标题/表格/列表保留）
│   ├── WordExtractor.java              # Word 提取器（docx 用 POI 本地解析，doc 回退 Tika）
│   ├── ExcelExtractor.java             # Excel/CSV 提取器（POI 全量 + FastExcel 流式 + CSV 状态机）
│   ├── AudioExtractor.java             # 音频元数据提取器（Tika Metadata）
│   ├── ImageExtractor.java             # 图片 OCR 提取器（调用本地 MinerU-OCR 视觉模型）
│   └── MarkdownAndTextExtractor.java   # Markdown/TXT 直接读取
├── chunker/                             # 文档分块策略（策略模式）
│   ├── Chunker.java                    # 分块器接口（supports + chunk）
│   ├── ChunkerFactory.java             # 工厂类（Spring Bean 自动发现，按 @Order 选择策略）
│   ├── SemanticChunker.java            # 语义分块 @Order(10)（多尺度滑动窗口 + 多策略动态阈值 + 父子分块）
│   ├── SeparatorChunker.java           # 分隔符分块 @Order(20)（正则/字面量 + Token 兜底细切）
│   └── TokenChunker.java               # Token 分块 @Order(30)（固定分块，兜底策略）
├── advisor/
│   ├── VectorStoreChatMemoryAdvisor.java       # 长期语义记忆 Advisor（Mem0 两阶段：提取→决策→应用；userId 跨会话 + 异步写入）
│   ├── RagContextQueryAdvisor.java             # 知识库上下文查询 Advisor（RAG 检索 + 注入系统提示词 + rag_retrieve 事件）
│   ├── HistoryChatMemoryAdvisor.java           # 全量历史记录 Advisor（sys_chat_history 入库 + 工具调用审计日志 + tool_call 事件推送）
│   ├── HistoryAwareToolSearchAdvisor.java      # 工具动态检索 Advisor（继承 ToolSearchToolCallingAdvisor，修复跨轮/压缩丢失工具回调）
│   ├── HistoryAwareToolCallingManager.java     # 执行期兜底装饰器（按 sessionId 从全量注册表补回工具回调）
│   ├── ReturnDirectChatMemoryAdvisor.java      # returnDirect 工具结果 Advisor
│   ├── WrappedMcpToolCallbackProvider.java     # MCP 工具包装器（为每个工具注入 returnDirect）
│   └── ReturnDirectToolCallbackWrapper.java    # 工具回调包装器（description 以 [JSON] 结尾 → returnDirect=true）
├── controller/
│   ├── AiConversationController.java   # 会话管理（创建/删除/列表）
│   ├── KbDocumentController.java       # 知识库文档管理
│   ├── KbDocumentChunkController.java  # 文档切片管理
│   ├── KbKnowledgeBaseController.java  # 知识库管理
│   └── SysChatHistoryController.java   # 聊天历史
├── service/
│   ├── IAiAgentToolCallLogService.java # 工具调用审计日志服务接口
│   └── impl/
│       ├── AiConversationServiceImpl.java      # 会话管理（创建+异步标题生成+级联删除）
│       ├── AiAgentToolCallLogServiceImpl.java  # 工具调用审计日志批量入库
│       ├── ChatAgentService.java               # LLM 流式阶段（RAG 已下沉到 Advisor）
│       ├── RagRetrieveContextService.java      # RAG 检索（供外部 API / NL2SQL 工具调用）
│       ├── RerankerService.java                # Reranker 重排序服务
│       ├── KbDocumentServiceImpl.java          # 文档上传流程编排（ExtractorFactory + ChunkerFactory）
│       └── ...
└── domain/
    ├── ChatStreamEvent.java            # SSE 事件结构
    ├── ChatRequest.java                # 聊天请求（question + conversationId + userId + tenantId + deptId + traceId）
    ├── SysChatHistory.java             # 聊天历史实体（全量记录）
    ├── AiConversation.java             # 会话实体（userId+conversationId+title）
    ├── AiAgentToolCallLog.java         # 工具调用审计日志实体（ai_agent_tool_call_log）
    ├── MemoryType.java                 # 记忆类型枚举（PROFILE 画像 / FACT 事实，对齐 Mem0）
    ├── MemoryOperation.java            # 记忆操作枚举（ADD / UPDATE / DELETE / NOOP）
    ├── ExtractedMemory.java            # 记忆提取阶段结构化产物（content + type）
    ├── KbDocument.java                 # 知识库文档实体
    └── ...
```

### 2.2 系统全局提示词（Nacos 提示词管理）

*   **实现方式**：提示词统一迁移到 **Nacos Prompt 管理**（`NacosPromptRegistry` + `PromptProperties`），由 Nacos 控制台在线编辑、版本管理、热发布，**不再使用本地 Markdown 文件**（`resources/prompts/*.md` 已移除）。
*   **加载机制**：应用启动时通过 `AiService.subscribePrompt()` 订阅绑定列表，Nacos 控制台发布新版本后**订阅回调自动刷新本地缓存**，无需重启服务。
*   **渲染方式**：`ChatAgentService` 每次调用实时读取本地缓存（`promptRegistry.get("system-prompt")`），渲染委托给 SDK 的 `Prompt.render(variables)`（调用方变量 > Nacos 控制台 defaultValue）。
*   **可靠性**：Last-Known-Good 策略——订阅事件解析失败时保留最近一次有效 Prompt 仅告警；`required=true` 的绑定启动加载失败直接报错，`required=false` 则忽略。
*   **配置要点**（`spring.ai.nacos.prompt.bindings`，每个绑定含 `key` / `version` / `label` / `required`）：

```yaml
spring:
  ai:
    nacos:
      prompt:
        server-addr: 114.132.102.8:8848   # Nacos 服务器
        namespace-id: public              # Prompt 所在命名空间
        username: nacos
        password: nacos
        transport-mode: http              # 可选，强制走 HTTP（默认 gRPC）
        bindings:
          system-prompt:                  # 业务别名（ChatAgentService 按此名读取）
            key: system-prompt
            label: latest
          VectorStoreChatMemoryPrompt:    # 长期记忆提取/决策提示词
            key: VectorStoreChatMemoryPrompt
            label: latest
          TitleCompressPrompt:            # 会话标题压缩提示词
            key: TitleCompressPrompt
            label: latest
          DecideInstructionPrompt:        # 记忆决策指令（Mem0 两阶段）
            key: DecideInstructionPrompt
            label: latest
          ExtractInstructionPrompt:       # 记忆提取指令（Mem0 两阶段）
            key: ExtractInstructionPrompt
            label: latest
```

> **架构演进（2026-09）**：原本地 `system-prompt-simplify.md` / `system-prompt.md` 文件下线，全部提示词（系统提示词、记忆提取/决策、标题压缩）统一由 Nacos 管控，支持**控制台热更新、版本回滚、MD5 去重**。

### 2.3 Advisor 链（核心拦截层）

Advisor 链是 Agent 编排的核心，7 个 Advisor 按 `order` 升序（值越小越先执行 before 阶段）依次装配（见 `ChatClientConfig.qwenChatClient()`）：

| 顺序 | Advisor | Order | 职责 |
| :--- | :--- | :--- | :--- |
| 1 | `VectorStoreChatMemoryAdvisor` | 98 | 长期语义记忆检索/写入（Weaviate，Mem0 两阶段）；仅 `vectorstore.enabled=true` 且 `conversationVectorStore` 存在时装配 |
| 2 | `ReturnDirectChatMemoryAdvisor` | 99 | 拦截 `returnDirect=true` 的工具结果，单独入库（在工具搜索之前执行） |
| 3 | `HistoryAwareToolSearchAdvisor` | 100 | 工具动态检索（渐进式披露，每次仅注入相关工具，详见 2.8.2） |
| 4 | `SessionMemoryAdvisor` | 101 | 近期上下文窗口记忆（Spring AI 2.0 `SessionService` + JDBC 存储 + 滑动窗口压缩） |
| 5 | `RagContextQueryAdvisor` | 102 | 知识库上下文查询（RAG 检索 + 注入系统提示词 + 推送 rag_retrieve 事件）；仅向量库开启时装配 |
| 6 | `HistoryChatMemoryAdvisor` | 103 | 全量聊天记录入库 MySQL + 工具调用审计日志 + tool_call 事件推送（在工具搜索之后，才能拿到工具调用信息） |
| 7 | `SimpleLoggerAdvisor` | 104 | 请求/响应观测日志 |

> **架构演进（2026-09）**：原 `MessageChatMemoryAdvisor` + `RedisCachedAndMysqlMemoryRepository` + `SpringAiChatMemory`（Redis+MySQL 双层窗口记忆）整体下线，改由 Spring AI 2.0 原生 **`SessionMemoryAdvisor` + `SessionService`（JDBC 仓储）** 承载近期上下文，并内置压缩策略；`FullHistoryChatMemoryAdvisor` 重命名为 `HistoryChatMemoryAdvisor` 并新增工具调用审计；`ToolSearchToolCallingAdvisor` 替换为增强子类 `HistoryAwareToolSearchAdvisor`。

#### 2.3.1 HistoryChatMemoryAdvisor（全量消息存储 + 工具审计）

**核心职责**：将每条有文本的消息（User/Assistant）全量存入 MySQL `sys_chat_history` 表，并将工具调用相关消息写入审计表 `ai_agent_tool_call_log`（`getName()` = “全量消息存储”）。

**关键逻辑**：
- **before 阶段**：将用户消息落库（`saveHistory`），同时写入工具调用审计（`saveToolCallLog`）
- **流式拦截**：检测 LLM 发起的工具调用，**遍历推送所有工具调用的 `tool_call` 事件**（`forEach`，避免并行工具只推送第一个）
- **after 阶段**：流式聚合完成后，将 Assistant 回复落库 + 写入审计日志
- **双轨落库（关注点分离）**：
  - **有文本的消息**（`StringUtils.hasText`）→ `sys_chat_history`（供前端展示）；`sequenceId` 由 Redis `INCR`（`Constants.SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId`）生成全局递增序号
  - **工具调用消息**（`AssistantMessage.hasToolCalls()` 或 `ToolResponseMessage`，且无正文文本）→ `ai_agent_tool_call_log`（供审计/追踪），每个工具调用展开为一条日志
- **审计日志字段**：`callId`（UUID）、`conversationId`、`userId`、`toolName`、`toolParams`（入参）、`resultDigest`（结果摘要）、`resultRef`（工具调用/响应的 JSON 序列化）、`status`（SUCCESS）；通过 `IAiAgentToolCallLogService.saveBatch()` 批量入库

#### 2.3.1.1 工具调用审计日志（AiAgentToolCallLog）

新增的 `ai_agent_tool_call_log` 表专门记录 Agent 每一次工具调用，与业务聊天记录（`sys_chat_history`）解耦：

| 字段 | 说明 |
| :--- | :--- |
| `callId` | 单次调用 ID（UUID） |
| `traceId` | 本轮请求的追踪 ID（贯穿单次请求的所有工具调用，来自 `ChatRequest.traceId`） |
| `conversationId` | 关联 `ai_conversation.conversation_id` |
| `userId` / `tenantId` | 触发调用的用户 / 租户隔离 |
| `toolName` | 工具名（如 `createSupplier`） |
| `toolParams` | 完整入参 |
| `resultDigest` | 结果摘要（`ReturnDirectToolCallbackWrapper` 侧最大 1024 字符截断） |
| `resultRef` | 大结果外置存储 key / 工具调用与响应的 JSON |
| `status` / `errorMsg` | 执行状态 / 错误信息 |
| `costMs` / `bizModule` | 执行耗时(ms) / 业务模块 |

**写入时机**：`HistoryChatMemoryAdvisor.saveToolCallLog()` 在 before（用户消息携带的工具调用）与 after（Assistant 发起的工具调用 + ToolResponseMessage）两阶段分别提取，按消息类型展开后批量入库。

#### 2.3.2 ReturnDirectChatMemoryAdvisor

**核心职责**：当工具标记了 `returnDirect=true`（`@Tool(returnDirect=true)`，description 以 `[JSON]` 结尾）时，工具结果不经过 LLM 处理直接返回前端；这类结果不走常规 `HistoryChatMemoryAdvisor` 的文本落库路径，需由本 Advisor（order=99，在工具搜索之前执行）单独拦截并**双写**入库。

**判断逻辑**：`before()` 为空操作，仅在 `after()` 检查 `ChatGenerationMetadata.finishReason == ToolExecutionResult.FINISH_REASON`（值为 `"returnDirect"`）时命中。

**双写落库**（对齐 0138f09 会话记忆重构）：

| 目标 | 写入方式 | 用途 |
| :--- | :--- | :--- |
| MySQL `sys_chat_history` | `saveBatch`；`sequenceId` 由 Redis `INCR`（`SEQ_CHAT_MEMORY_KEY_PREFIX + conversationId`）生成 | 前端历史展示 |
| `SessionService`（JDBC 会话存储） | `appendEvent(SessionEvent)`，`sessionId = conversationId` | 使 returnDirect 结果进入模型滑动窗口记忆，后续轮次可感知 |

#### 2.3.3 VectorStoreChatMemoryAdvisor（长期语义记忆）

**核心职责**：将每一轮对话异步提取为“记忆事实”写入 Weaviate，并在进入模型前按 **userId** 检索生效中的长期记忆注入系统提示词。**作用域为 userId，长期记忆跨会话生效**。

**记忆模型（对齐 Mem0）**：记忆按 `MemoryType` 二分，写入按 `MemoryOperation` 四种操作决策：

| 记忆类型 | 含义 | 更新策略 |
| :--- | :--- | :--- |
| `PROFILE` | 用户画像：身份、姓名、长期偏好、工作、城市等稳定属性 | **覆盖**（UPDATE） |
| `FACT` | 对话事实：事件、决定、临时意图、任务 | **追加**（ADD，保留历史） |

| 操作 | 含义 |
| :--- | :--- |
| `ADD` | 新增一条记忆 |
| `UPDATE` | 用新内容替换某条旧记忆（需 `targetId`） |
| `DELETE` | 删除某条旧记忆（需 `targetId`） |
| `NOOP` | 无需操作（重复/无意义） |

**两阶段处理流水线（提取 → 决策 → 应用）**：

```
after() 阶段拿到本轮完整对话（用户消息 + AI 回复）
  │
  ├─ Phase 1 提取（extractMemories）
  │   ├─ 输入：【用户消息】(≤400字) + 【AI回复】(≤300字)，上下文感知
  │   ├─ 调用 smallChatClient + EXTRACT_INSTRUCTION，输出严格 JSON 数组
  │   │   [{"content":"用户叫张三","type":"profile"}, ...]
  │   ├─ 原子事实拆分（一句一事），每条以“用户”开头、≤字数限制
  │   ├─ 寒暄/闲聊/无实质信息 → 返回空数组，本轮不写
  │   └─ 单轮最多 MAX_MEMORIES_PER_TURN=5 条
  │
  └─ Phase 2 逐条决策应用（decideAndApply）
      ├─ 检索决策候选（searchDecisionCandidates）：top-3 + 相似度阈值 0.75
      ├─ 无候选 → ADD（直接写入）
      ├─ 候选中存在文本完全相同 → 续期（写新 + 删旧，刷新 TTL）
      └─ 否则调用 smallChatClient + DECIDE_INSTRUCTION 得到 {op, target_id, content}：
          ├─ ADD    → writeMemory
          ├─ UPDATE → writeMemory(新内容) + deleteMemory(targetId)   # profile 覆盖
          ├─ DELETE → deleteMemory(targetId)                        # 新记忆否定/撤销旧记忆
          └─ NOOP   → 跳过（重复）
```

**关键设计**：

| 设计点 | 说明 |
| :--- | :--- |
| 作用域 | `userId`（跨会话检索主过滤字段），`conversationId` 仅随 metadata 落库用于追踪 |
| 异步写入 | `storeMemoryAsync()` 运行在 advisor 自身 scheduler 上，不阻塞请求链路，首 token 延迟不受提取/决策/写入影响 |
| 上下文感知提取 | 输入为“本轮用户消息 + AI 回复”，解决“那上海的呢？”这类依赖上下文的指代消息；AI 回复仅用于消歧/补全执行结果，禁止将 AI 推理/建议作为记忆 |
| 提取/决策分离 | Phase1 只负责“提取什么”，Phase2 只负责“如何入库”，两个专用 prompt（EXTRACT_INSTRUCTION / DECIDE_INSTRUCTION）职责单一 |
| 类型驱动更新 | profile 冲突走 UPDATE 覆盖；fact 冲突走 ADD 追加，保留历史轨迹 |
| 记忆有效期 | 默认 30 天（`DEFAULT_MEMORY_TTL_MS`），写入时推导 `expireAt`；检索时过滤 `expireAt > now`，过期记忆不注入模型 |
| 永不过期 | TTL=0 的“永不过期”用远期时间戳（`NEVER_EXPIRE_EXPIRE_AT_MS`，100 年）落地，避免被 `gt(expireAt, now)` 过滤排除 |
| 敏感信息脱敏 | 身份证/手机号/银行卡等敏感信息自动脱敏为 `***` 后再入库 |
| 输出格式约束 | 提取/决策指令均强制严格 JSON、禁止代码块标记与转义引号，`sanitizeJsonOutput()` 容错解析 |
| 降级 | 向量库不可用/LLM 调用失败/JSON 解析失败时降级为无记忆继续对话，不中断请求 |
| 记忆文本长度上限 | 小模型输出超过 `MAX_MEMORY_TEXT_LENGTH=100` 字符视为异常，按失败处理 |
| 时序排序 | 检索命中后 `rankByRecency()` 按时间排序再注入，提升近期记忆权重 |

**记忆 Schema 字段**（与 `VectorStoreConfig` 会话记忆库对齐，常量收敛到 `ChatConstants`）：

| 字段 | 说明 |
| :--- | :--- |
| `userId` | 记忆归属用户 ID，长期记忆跨会话检索的基础过滤字段 |
| `conversationId` | 归属会话 ID，仅随 metadata 落库用于追踪 |
| `messageType` | 消息角色（USER / ASSISTANT / SYSTEM） |
| `memory_type` | 记忆类型（profile / fact），决策阶段据此选择覆盖或追加 |
| `status` | 记忆状态（active / archived / superseded / expired，用字符串而非 boolean） |
| `ingestedAt` | 记忆写入时间戳（毫秒） |
| `expireAt` | 记忆过期时间戳（毫秒），0 表示不过期 |

**检索过滤表达式**：`userId = {userId} AND status = active AND expireAt > now`

#### 2.3.4 SessionMemoryAdvisor（近期上下文窗口记忆）

**核心职责**：基于 Spring AI 2.0 原生会话 API（`SessionService` + `SessionMemoryAdvisor`）管理近期上下文窗口，**取代原自定义的 `RedisCachedAndMysqlMemoryRepository` + `SpringAiChatMemory` 双层存储方案**。

**装配参数**（`ChatClientConfig.sessionMemoryAdvisor()`）：

| 参数 | 值 | 说明 |
| :--- | :--- | :--- |
| `SessionService` | JDBC 仓储 | 会话持久化（`spring.ai.session.repository.jdbc.initialize-schema=always` 自动建表） |
| `compactionTrigger` | `TurnCountTrigger(4)` | 每 4 轮对话触发一次压缩 |
| `compactionStrategy` | `SlidingWindowCompactionStrategy(maxEvents=8)` | 滑动窗口压缩，保留最近 8 个事件 |
| `order` | 101 | 在工具搜索之后、RAG 之前执行 |
| `time-to-live` | 30d | 会话有效期（`spring.ai.session.time-to-live`，支持 ISO-8601） |

**与旧方案的区别**：
- 旧：`MessageChatMemoryAdvisor` + 自实现 `ChatMemoryRepository`（Redis 缓存 + MySQL 回填 + fastjson2 `@type` 序列化），窗口大小由 `chat-memory.max-messages` 控制
- 新：框架原生 `SessionService` 接管存储与生命周期，内置**轮次触发 + 滑动窗口压缩**，无需自建双层仓储；`chat-memory.max-messages` 配置已移除

### 2.4 会话管理与级联操作

#### 2.4.1 会话创建与异步标题生成

`AiConversationServiceImpl.createAiConversation()` 流程：

1. 调用 `sessionService.create(CreateSessionRequest{userId})` 创建会话，以返回的 `session.id()` 作为 conversationId，建立 userId 与 conversationId 的关联
2. **直接将用户第一条消息 `question` 作为会话标题**写入 `ai_conversation.title`
3. 写入 Redis（`chat:conversation:{conversationId}` → userId，TTL 7天）
4. 前端在已有有效标题（非空、非"未命名对话"）时不再覆盖标题，仅对空标题或占位符生成新标题

**标题生成（smallChatClient）**：
- **仅当 `question.length() > 20` 时**才使用 `smallChatClient`（概述小模型，替代原 `titleChatClient`）异步生成标题（短问题直接用原文，避免浪费 LLM 资源）；生成前二次检查会话是否已有标题，避免重复调用
- 内置标题生成 System Prompt：不超过 15 字、概括主题、不要标点结尾、只输出标题本身
- **后处理**：取首行、去空白、截断到 30 字，防止模型输出多余内容污染标题

**关键类**：`AiConversationServiceImpl`、前端 `index.vue`（标题覆盖保护逻辑）

#### 2.4.2 会话级联删除

`deleteByConversationId()` 执行 5 层级联清理：

| 层级 | 操作 | 存储位置 |
| :--- | :--- | :--- |
| 1. 会话记忆 | `sessionService.delete(conversationId)` 删除近期上下文会话 | SessionService（JDBC） |
| 2. 关联表 | 删除 userId ↔ conversationId 关联 | MySQL `ai_conversation` |
| 3. 全量历史 | 删除所有聊天记录 | MySQL `sys_chat_history` |
| 4. 工具审计 | 删除工具调用审计日志 | MySQL `ai_agent_tool_call_log` |
| 5. 向量数据 | 删除会话向量 + 工具索引向量 | Weaviate（conversationId + sessionId 过滤） |

**Redis Key 清理**：
- `chat:conversation:{conversationId}` — 会话关联
- `seq:chat:memory:{conversationId}` — 消息序列号

> **架构演进**：原“窗口记忆”层（`spring_ai_chat_memory` + `chat:memory:{conversationId}` 缓存）已随 `SpringAiChatMemory` 体系下线，改由 `sessionService.delete()` 统一清理；新增工具审计日志（`ai_agent_tool_call_log`）的级联删除。

#### 2.4.3 SaLlmConfig 配置

`SaLlmConfig` 定义了两个核心 Bean（原 `chatMemory` Bean 已移除，窗口记忆改由 `SessionMemoryAdvisor` + `SessionService` 承载）：

| Bean | 类型 | 说明 |
| :--- | :--- | :--- |
| `mineruRestClient` | `RestClient` | MinerU API 客户端（连接超时10s，读取超时60s） |
| `taskExecutor` | `Executor` | 异步线程池（核心5/最大10/队列100，`CallerRunsPolicy`），用于标题生成等异步任务 |

### 2.5 会话记忆与上下文管理

#### 2.5.1 多用户会话隔离

*   **隔离维度**：`userId`（用户维度的数据权限） + `conversationId`（单次会话维度的上下文连续性）。
*   **实现机制**：
    *   当用户第一次输入内容进行请求，创建 conversationId 并与用户 id 建立关联关系
    *   通过 `ChatMemory.CONVERSATION_ID` 上下文传递会话 ID（作为 `SessionMemoryAdvisor` / `HistoryAwareToolSearchAdvisor` 的 sessionId），通过 `SessionMemoryAdvisor.USER_ID_CONTEXT_KEY` 传递用户 ID
    *   在所有存储层（SessionService JDBC 会话、MySQL `sys_chat_history` / `ai_agent_tool_call_log`、VectorStore）的数据写入时，必须带上 CONVERSATION_ID 与 userId

#### 2.5.2 上下文窗口（近期记忆 - Window Memory）

这是 AI "正在看"的内容，决定了对话的连续性。

*   **实现**：由 Spring AI 2.0 原生 `SessionMemoryAdvisor` + `SessionService`（JDBC 仓储）承载，取代原 `MessageChatMemoryAdvisor` + `RedisCachedAndMysqlMemoryRepository` 自建双层存储。
*   **压缩策略**：`TurnCountTrigger(4)` 每 4 轮触发一次压缩，`SlidingWindowCompactionStrategy(maxEvents=8)` 保留最近 8 个事件，超出部分自动压缩。
*   **存储与生命周期**：会话持久化到 JDBC（`initialize-schema=always` 自动建表），有效期 `time-to-live=30d`，过期自动清理。

#### 2.5.3 向量库全量上下文（长期记忆 - Vector Memory）

这是 AI "能想起"的内容，实现了跨会话、长期的语义检索。

*   **应用场景**：用户询问"我最初问了什么"，即使该对话不在当前窗口，AI 也能通过向量检索找到。
*   **技术栈**：本地 Embedding 模型（Qwen3-Embedding-4B） + Weaviate 向量数据库。
*   **检索逻辑**：基于 `VectorStoreChatMemoryAdvisor`，按 **userId** 检索生效中的长期记忆（`status=active AND expireAt > now`）。
    *   **参数**：`defaultTopK`（配置：`vectorstore.chat-memory-default-topk: 1`）。
*   **记忆写入（异步）**：本轮完整对话（用户消息 + AI 回复）经 `smallChatClient` 提取为以“用户”开头的原子事实短句后异步写入向量库，不阻塞请求链路。
*   **记忆决策（Mem0 模型）**：写入时按相似度检索候选（top-3 + 阈值 0.75），由小模型判定 `ADD/UPDATE/DELETE/NOOP`：profile 冲突覆盖、fact 冲突追加、文本完全相同则续期 TTL。
*   **记忆有效期**：默认 30 天（`expireAt` 字段），过期记忆不注入模型。
*   **只存用户视角记忆**：提取以用户消息为主，AI 回复仅用于消歧/补全执行结果，避免通用知识/任务结果污染记忆库。
*   **架构演进**：原 `VectorCompressionService`（批量压缩）已移除，由上述“提取→决策→应用”的 Mem0 两阶段机制取代。

#### 2.5.4 全量聊天记录（业务展示）

*   **作用**：供前端展示"历史会话列表"和"聊天详情"，支持分页、关键词搜索。
*   **存储**：MySQL 业务表 `sys_chat_history`，包含 `conversation_id`、`content`、`sequence_id`、`type`、`timestamp`、`create_by`、`create_time`、`update_by`、`update_time` 等字段。
*   **特点**：**全量永久存储**（除非用户主动删除），不进行滑动窗口截断。
*   **重点类**：`HistoryChatMemoryAdvisor`。

### 2.6 RAG 检索引擎

#### 2.6.0 知识库类型区分（专用 / 通用）

知识库通过 `kb_knowledge_base` 表的 `kb_type` 字段区分类型，RAG 检索时**先按 kbType 定位知识库范围，再执行标签匹配与向量检索**：

| kbType | 类型 | 适用场景 | 检索参数 |
| :--- | :--- | :--- | :--- |
| `10` | **通用知识库** | ChatAgent 对话默认注入的通用参考知识 | `RagContextQueryAdvisor` 默认走 10（可通过 context key `rag_kb_type` 覆盖） |
| `20` | **专用知识库** | NL2SQL 表结构等特定业务域的专用知识 | `Nl2SqlToolServiceImpl` 通过 Feign 调用 chat 服务时显式传 `kbType=20` |

**设计要点**：

- `kb_type` 定义在知识库表（`kb_knowledge_base`），文档（`kb_document`）通过 `knowledge_id` 关联所属知识库；
- RAG 检索第一步通过 `selectDocumentsByKbType(kbType, status="0")` **两表关联一次查询**，只获取该类型下所有启用知识库的文档，避免"查知识库 + 查文档"两次查询开销；
- 文档的 `tags` 字段（多个用逗号分隔）用于**先查询 tag 再确定检索范围**，匹配到的 `knowledgeId` 集合作为后续向量检索的必选过滤条件。

#### 2.6.1 三步检索流程（标签匹配 → 单重过滤向量检索 → Reranker 重排序）

> **架构演进**：知识库查询逻辑已从 `ChatAgentService.ragPhase()` 迁移到 **`RagContextQueryAdvisor`**（参考 `VectorStoreChatMemoryAdvisor` 的 Advisor 模式）。Advisor 在 `before` 阶段执行检索，将结果注入系统提示词，并通过 `AgentEventSinkManager.emitRagRetrieve()` 推送 `rag_retrieve` 状态事件（start / success / empty）。`RagRetrieveContextService` 保留供外部 API（`KbRagRetrieveApi`）和 NL2SQL 工具（kbType=20）调用。

`RagContextQueryAdvisor.retrieveContext()` 采用 **"先查询 tag + 单重过滤 + 重排序"** 三步检索策略，对标 Dify Knowledge Retrieve API：

```
用户问题
  │
  ├─ 第一步: 先查询 tag 再执行（先按 kbType 定位 → 找出所有相似的 KbDocument）
  │   ├─ 0. 知识库前置检查：selectDocumentsByKbType(kbType, "0") 两表关联一次查询
  │   │     → 获取该 kbType 下所有"启用"知识库的文档；无启用知识库则直接跳过检索
  │   ├─ 1. 反向匹配（matchDocsByReverseTag）：纯内存计算
  │   │     → 检查 question 是否包含某个 tag（字符串包含，准确率100%，至少2字符）
  │   │     → 命中则提取精确 tags 值 + getKnowledgeId()
  │   ├─ 2. 模糊匹配（matchDocsByFuzzyMatch）：反向匹配未命中时
  │   │     → 按中英文逗号/空格拆分 question 为关键词（至少2字符）
  │   │     → 在内存中检查 tags 是否包含任一关键词（提升召回率，不走 SQL LIKE）
  │   └─ 3. 全量降级：标签匹配均未命中
  │         → 退化为仅 knowledgeId 过滤的全量检索
  │
  ├─ 第二步: 向量检索（仅 knowledgeId 单重过滤）
  │   ├─ 固定过滤条件：knowledgeId IN (...)
  │   ├─ 启用 Reranker：topK=10, 相似度阈值 0.5（召回更多候选）
  │   └─ 未启用 Reranker：topK=3, 相似度阈值 0.7
  │
  ├─ 第三步: Reranker 重排序（精排）
  │   ├─ 调用 127.0.0.1:8887/v1/rerank
  │   ├─ 对候选文档按相关性重新排序，统一截取 topN=3 条
  │   └─ 失败时降级为向量检索前 3 条
  │
  └─ 返回上下文 → 注入到 LLM
```

> **说明**：`kbType` 用于区分知识库类型（10-通用、20-NL2SQL专用表结构等）。第一步通过 `selectDocumentsByKbType(kbType, "0")` **两表关联一次查询**获取该类型下所有启用知识库的文档，然后**先做 tag 匹配（反向匹配 + 模糊匹配）**确定精确的 `getKnowledgeId()` 集合，用于第二步向量检索的 `knowledgeId IN (...)` 单重过滤——标签匹配只用于**定位知识库范围**，不再叠加 tags 过滤，简化执行链并避免误过滤。

**配置项**：
```yaml
reranker:
  enabled: false           # 是否启用重排序
  base-url: http://127.0.0.1:8887
  top-n: 3
```

**关键实现方法**（`RagRetrieveContextService`）：

| 方法 | 职责 |
| :--- | :--- |
| `matchDocumentsByTags()` | 第一步：先查 kbType 下启用文档，再做标签匹配，含四级流程（前置检查→反向匹配→内存模糊匹配→全量降级） |
| `matchDocsByReverseTag()` | 反向匹配核心：检查 question 是否包含 tag（至少2字符为限，防单字误匹配，纯内存） |
| `matchDocsByFuzzyMatch()` | 内存模糊匹配：将 question 按中英文逗号/空格拆分为关键词，检查 tags 是否包含任一关键词（替代 SQL LIKE，零SQL开销） |
| `vectorSearch()` | 第二步：向量检索，根据是否启用 Reranker 动态调整 topK 与阈值 |
| `rerankAndTrim()` | 第三步：Reranker 重排序 + 统一截取 TopN=3 |
| `MatchResult` | 匹配结果封装（tagValues + knowledgeIds），贯穿三步流程 |

#### 2.6.2 知识库文档处理

文档上传后的处理流程：

```
文件上传 → RemoteFileService 获取文件
  │
  ├─ ExtractorFactory.getExtractor(filename)  # 按文件后缀自动路由
  │   ├─ .md/.txt        → MarkdownAndTextExtractor（直接 Files.readString）
  │   ├─ .pdf            → PdfExtractor（Tika 结构化 Markdown，标题/表格/列表保留）
  │   ├─ .doc/.docx      → WordExtractor（docx: POI 本地解析; doc: 回退 Tika）
  │   ├─ .xls/.xlsx/.csv → ExcelExtractor（POI全量/FastExcel流式/CSV状态机）
  │   ├─ 图片(png/jpg/gif) → ImageExtractor（本地 MinerU-OCR 视觉模型）
  │   └─ 音频(mp3/wav/flac) → AudioExtractor（Tika Metadata 提取）
  │
  ├─ 内容清洗 (DocumentCleaner: 移除 OCR 噪声、页码、乱码)
  ├─ ChunkerFactory.chunk(document, semanticEnabled, chunkSize, chunkSeparator)  # 分块策略路由（按 @Order）
  │   ├─ semanticEnabled=true   → SemanticChunker @Order(10)（多尺度滑动窗口 + 多策略动态阈值 + 父子分块）
  │   ├─ semanticEnabled=false + 分隔符非空 → SeparatorChunker @Order(20)（正则/字面量 + Token 兜底）
  │   └─ semanticEnabled=false + 分隔符为空 → TokenChunker @Order(30)（TokenTextSplitter 固定分块）
  ├─ 存入 Weaviate 向量库
  └─ 同步 MySQL chunk 记录
```

**知识库向量存储字段**（`knowledgeVectorStore` Bean 定义的 metadata Schema，常量收敛到 `ChatConstants`）：

| 字段 | 类型 | 写入方 | 说明 |
| :--- | :--- | :--- | :--- |
| `knowledgeId` | text | `KbDocumentServiceImpl` | 知识库归属 ID，RAG 检索/删除的基础过滤字段 |
| `source` | text | `KbDocumentServiceImpl` | 文档来源标识（文件路径） |
| `filename` | text | `KbDocumentServiceImpl` | 文档文件名 |
| `docType` | text | 预留 | 文档类型（如 manual / faq / policy） |
| `chunkIndex` | number | 预留 | 分块序号 |
| `version` | number | 预留 | 知识版本号 |
| `updatedAt` | number | 预留 | 知识更新时间戳（毫秒） |

> **说明**：当前 `KbDocumentServiceImpl.insertKbDocument()` 写入向量库时携带 `filename`、`knowledgeId`、`source` 三个字段；`docType`、`chunkIndex`、`version`、`updatedAt` 为 Schema 预留字段，后续写入方落地后即可直接过滤，无需变更 Schema。

##### 2.6.2.1 Extractor 策略模式（文档内容提取）

采用 **接口 + 工厂 + 策略实现** 的经典设计模式，新增文件类型只需实现 `Extractor` 接口并注册为 Spring Bean，工厂自动发现并参与路由：

| 提取器 | 支持格式 | 解析方式 | 关键特性 |
| :--- | :--- | :--- | :--- |
| `MarkdownAndTextExtractor` | .md, .txt | `Files.readString` | 零开销直读 |
| `PdfExtractor` | .pdf | Tika XHTML SAX → Markdown | 标题层级/表格/列表结构保留；输出上限防 OOM；文件头魔数验证 |
| `WordExtractor` | .docx, .doc | docx: POI XWPF 本地解析; doc: Tika 回退 | 标题四级匹配、列表缩进、合并单元格填值、内嵌图片 MD5 去重 + 并行 OCR、图片哨兵占位符 |
| `ExcelExtractor` | .xls, .xlsx, .xlsm, .csv | 小文件: POI 全量; 大xlsx(≥10MB): FastExcel 流式; CSV: RFC4180 状态机 | 编码自动嗅探(UTF-8/GB18030)、合并单元格填值、隐藏sheet跳过、Tika兆底 |
| `ImageExtractor` | .png, .jpg, .jpeg, .gif | 本地 MinerU-OCR 视觉模型 | opendatalab/MinerU2.5-Pro-2605-1.2B @ 127.0.0.1:8890 |
| `AudioExtractor` | .mp3, .wav, .ogg, .flac, .m4a, .aac 等 | Tika Metadata | 提取标题/艺术家/专辑/时长/比特率等元数据 |

**AbstractTikaExtractor 抽象基类**（PdfExtractor / WordExtractor / ExcelExtractor 共用）：

| 解析路径 | 方法 | 说明 |
| :--- | :--- | :--- |
| 结构化 Markdown | `parseTikaToMarkdown(Path)` | XHTML SAX 事件流式转 Markdown，保留标题/表格/列表结构 |
| 纯文本（兆底） | `parseWithTikaLimited(Path)` | 带输出上限的纯文本提取 |

**降级策略**：

| 场景 | 降级行为 |
| :--- | :--- |
| 文档超过输出上限（默认 2M 字符） | 返回部分内容 + “[文档过大，内容已截断]” |
| 解析失败（加密/损坏） | 返回 “[文档解析失败]” 占位文案，不抛异常 |
| 主路径失败（Excel） | Tika 纯文本兆底；Tika 也失败则还原原始异常 |
| 扫描件（PDF 无文本层） | 输出接近空串，建议调用方转 OCR 链路 |

**关键配置**：
```yaml
extract:
  tika-max-chars: 2097152    # Tika 解析输出上限（2M 字符 ≈ 4MB 内存）
  images: true               # Word 图片提取总开关（批量灌库时可关闭提速）
  page-break-mark: false     # Word 分页标记开关
```

##### 2.6.2.2 WordExtractor 详细设计

`WordExtractor` 是最复杂的提取器（972 行），分两条路径：

| 路径 | 格式 | 解析方式 | 输出质量 |
| :--- | :--- | :--- | :--- |
| POI 本地解析 | .docx | XWPF + XmlCursor 递归遍历 | 高（结构完整） |
| Tika 回退 | .doc | XHTML SAX → Markdown | 中（结构部分保留） |

**docx POI 解析核心能力**：

| 功能 | 实现细节 |
| :--- | :--- |
| 标题识别 | 四级匹配：styleId=headingN → 数字styleId(交叉验证) → 样式名含"标题/heading" → outlineLvl大纲级别；另有无样式标题降级检测（全加粗+大字号+短文本） |
| 列表缩进 | numPr 项目符号/编号，每级 2 空格（引用基类 `LIST_INDENT_UNIT` 常量） |
| 表格合并 | gridSpan/vMerge 填值，单元格文本按实例缓存，嵌套表格转紧凑键值对 |
| 图片处理 | blip@embed/imagedata@id → rId → 图片；内容 MD5 去重；小图过滤(<5KB)；共享线程池并行 OCR；总超时 120s |
| 图片占位符 | 私用区哨兵字符(\uE000...\uE001)，不与正文碰撞 |
| 特殊元素 | hyperlink/smartTag/sdt/fldSimple 内文本；过滤删除修订(del/delText)；AlternateContent 只取 Choice 分支 |
| 生成器兼容 | `<t>` 文本改用 `cursor.getTextValue()` 读取（不强转 `CTText`），兼容 WPS/LibreOffice 在扩展命名空间或 Fallback 分支产出的 `<t>`——Schema 未命中时 `getObject()` 返回 `XmlAnyTypeImpl`，强转会抛 `ClassCastException` |
| 内容控件 | XWPFSDT 兆底取文本 |

##### 2.6.2.3 ExcelExtractor 详细设计

按文件类型/大小分流解析为 Markdown 表格：

| 路径 | 触发条件 | 解析方式 |
| :--- | :--- | :--- |
| POI 全量 | 小文件(<10MB) / .xls / .xlsm | WorkbookFactory + DataFormatter + FormulaEvaluator |
| FastExcel 流式 | 大 .xlsx(≥10MB) | SAX 逐行读取，避免 OOM |
| CSV 状态机 | .csv | RFC4180 解析（引号内换行续读、双引号转义） |
| Tika 兆底 | 主路径失败 | 纯文本 + 输出上限 |

**关键参数**：

| 参数 | 值 | 说明 |
| :--- | :--- | :--- |
| `STREAMING_THRESHOLD` | 10MB | 超过则走 FastExcel 流式解析 |
| `MAX_CELL_CHARS` | 1000 | 单元格最大字符数，超长截断 |
| `MAX_ROWS_PER_SHEET` | 10000 | 单 sheet 最大数据行数，超出截断并标注 |
| CSV 编码嗅探 | UTF-8 BOM → 严格UTF-8 → GB18030 | 自动检测文件编码 |

##### 2.6.2.4 内容清洗策略（cleanContent）

提取文本后执行 5 步规则清洗，移除 OCR 噪声：

| 步骤 | 正则 | 作用 |
| :--- | :--- | :--- |
| 1 | `(<\|txt_contd_tgt\|>\|<\|txt_contd_src\|>)+` | 移除 MinerU 特殊续文本标签 |
| 2 | `(?m)^\s*\d+\s*$` | 移除仅含数字的独立行（通常是页码） |
| 3 | `[■□▲△○●⊛※]{2,}` | 移除连续 2+ 个 OCR 噪声字符 |
| 4 | `([\u4e00-\u9fa5])\s+([\u4e00-\u9fa5])` → `$1$2` | 去除中文字符之间的多余空格 |
| 5 | `\n{3,}` → `\n\n` | 规范化空行，最多保留一个空行 |

最后执行 `trim()` 去除首尾空白。清洗后内容为空则抛出异常终止处理。

##### 2.6.2.5 分块策略（Chunker 策略模式）

分块逻辑已从 `KbDocumentServiceImpl` 独立为 **`chunker` 包**，采用与 Extractor 相同的策略模式设计：

| 组件 | 职责 |
| :--- | :--- |
| `Chunker` 接口 | 定义 `supports(semanticEnabled, chunkSize, chunkSeparator)` + `chunk(document, chunkSize, chunkSeparator)` |
| `ChunkerFactory` 工厂 | Spring Bean 自动发现所有 Chunker，按 `@Order` 优先级选择；无匹配时默认回退 TokenChunker |
| `SemanticChunker` `@Order(10)` | 语义分块（多尺度滑动窗口 + 动态阈值），分隔符作为最小单元切分依据 |
| `SeparatorChunker` `@Order(20)` | 自定义分隔符分块（正则/字面量），超长块用 TokenTextSplitter 兜底细切 |
| `TokenChunker` `@Order(30)` | Token 固定分块（TokenTextSplitter），兜底策略 |

**调用方式**：`KbDocumentServiceImpl` 通过 `ChunkerFactory.chunk(document, semanticEnabled, chunkSize, chunkSeparator)` 一行代码完成分块，无需感知内部策略。

**策略选择决策表**：

| semanticEnabled | chunkSeparator | 命中策略 |
| :---: | :---: | :--- |
| `true` | 任意 | `SemanticChunker`（分隔符作为最小单元切分依据；EmbeddingModel 缺失时降级 Token） |
| `false` | 非空 | `SeparatorChunker`（按分隔符切分 + Token 兜底细切） |
| `false` | 空 | `TokenChunker`（固定 Token 分块） |

**策略 A：TokenChunker（固定分块，兜底）**

使用 Spring AI 的 `TokenTextSplitter`，关键参数：

| 参数 | 值 | 说明 |
| :--- | :--- | :--- |
| `chunkSize` | `kbDocument.chunkSize`，默认 500 | 单块最大 Token 数 |
| `minChunkSizeChars` | `chunkSize * 0.1`（即 50） | 块最小字符数，保证完整性 |
| `minChunkLengthToEmbed` | 5 | 小于此长度的块跳过向量化 |
| `maxNumChunks` | 10000 | 最大块数上限 |
| `keepSeparator` | true | 保留分隔符 |
| `punctuationMarks` | `. ? ! 。？ ！ \n ; ；` | 标点切分支持（中英文） |
| `encodingType` | `CL100K_BASE` | Token 编码方式（GPT 系列） |

**策略 B：SeparatorChunker（自定义分隔符分块）**

通过 `kb_document.chunk_separator` 字段配置分隔符（正则或字面量），适用于结构规整的文档：Markdown 标题（`(?m)^#{1,3}\s`）、代码函数（`\n\s*(public|private|def)\s`）、FAQ（`\n\s*Q[:：]`）、章节标记（`\n---+\n`）等。

**算法流程**：

```
文档内容 + chunkSeparator（正则）
  │
  ├─ 1. Pattern.compile(separator).split(text) 按分隔符切分为原始块
  │   └─ trim 后过滤空串；若切分结果为空，降级 Token 分块
  │
  ├─ 2. 过小的块（< MIN_UNIT_TOKENS=100 Token）向前合并
  │   └─ 合并后 Token 数不超过 chunkSize 才允许合并，避免超限
  │
  └─ 3. 超长块（Token 数 > chunkSize）用 TokenTextSplitter 兜底细切
      └─ 保留原始 metadata，输出 Document 列表
```

**关键参数**：

| 参数 | 值 | 说明 |
| :--- | :--- | :--- |
| `MIN_UNIT_TOKENS` | `100` | 块最小 Token 数（统一 token 度量，中英文行为一致），低于此值向前合并 |
| `MERGE_JOINER` | `\n\n` | 合并块时的连接符 |
| Token 细切分参数 | 同 TokenChunker | 超长块兜底 |

**策略 C：SemanticChunker（语义分块 V5 —— 多尺度滑动窗口 + 多策略动态阈值 + 父子分层分块 + 块间重叠）**

通过 `kb_document.semantic_chunking` 字段控制启用；若同时配置了 `chunk_separator`，则作为**最小语义单元**的切分依据（未配置时默认按 `\n\n` 段落切分）。

**核心思想**：先做**结构感知切分**（标题 → 段落 → 句子）得到语义最小单元，基于 Embedding 向量用**多尺度滑动窗口**计算相邻单元相似度，再用**多策略动态阈值**（默认标准差法）检测语义边界；语义相近的单元聚合为**父块**，父块再切分为带重叠的**子块**用于向量检索——命中子块后通过 metadata 回溯父块全文喂给 LLM（对齐 Dify 父子分块模式）。全流程以 **Token** 为统一度量（jtokkit CL100K_BASE），并对相邻块注入句子级重叠缓解边界割裂。

**算法流程**（`SemanticChunker.chunk()`）：

```
文档内容 + chunkSeparator（可选）
  │
  ├─ 0. 文本清洗（cleanText）
  │   ├─ 换行统一 CRLF/CR → LF、连续空白归一、空行压缩至 ≤2
  │   └─ 删除 URL / 邮箱（REMOVE_URL_AND_EMAIL=true，对齐 Dify 预处理）
  │
  ├─ 1. 结构感知切分为语义最小单元（splitIntoSemanticUnits）
  │   ├─ 先按标题行分 section（HEADING_PATTERN），再按 chunkSeparator（默认空行）分段落
  │   ├─ 段落 > chunkSize*2（MAX_UNIT_TOKENS_FACTOR）时按句子聚合，单元 ≥ MIN_UNIT_TOKENS=100
  │   └─ startIndex 游标递进定位（O(n)，重复段落各自定位到正确位置）
  │
  ├─ 2. 窗口大小自适应：windowSize = min(MAX_WINDOW_SIZE=2, (units-1)/2)
  │
  ├─ 3. 计算单元 Embedding（computeEmbeddings）
  │   ├─ 超长单元按句子预切分至 ≤ embeddingMaxInputTokens=512（单句超限退 token 硬切并修复 U+FFFD）
  │   ├─ 分批调用（EMBEDDING_BATCH_SIZE=16），多片段按维平均为整单元向量
  │   └─ 返回向量数与请求片段数校验，异常或不符则整体降级 Token 分块
  │
  ├─ 4. 多尺度滑动窗口找切分点（findSplitPoints）
  │   ├─ 每个位置 p：左右各 windowSize 个单元的平均向量余弦相似度，多尺度取均值
  │   ├─ 多策略动态阈值（THRESHOLD_STRATEGY）选出候选断点：
  │   │   • STANDARD_DEVIATION（默认）：阈值 = mean - k*std（k=THRESHOLD_AMOUNT=1.5）
  │   │   • PERCENTILE：相似度最低的 PERCENTILE_FRACTION=0.20 分位
  │   │   • INTERQUARTILE：阈值 = Q1 - k*(Q3-Q1)
  │   │   • GRADIENT：相邻相似度骤降幅度最大的 5% 位置
  │   └─ 局部极小值校验（LOCAL_MIN_RADIUS=1）剔除窄幅抖动造成的噪声切分
  │
  ├─ 5. 组装父块（groupUnitsBySplitPoints → splitOversizedGroups → mergeSmallGroups）
  │   ├─ 按切分点分组
  │   ├─ 超长组（> chunkSize*2）按单元边界二次拆分
  │   └─ 过小组（< MIN_UNIT_TOKENS=100）就近双向合并（合并后不超 chunkSize）
  │
  ├─ 6. 物化父块（toParentChunk）：标题拼入父块文本，保证上下文完整 + 标题式 query 召回
  │
  ├─ 7. 父块间重叠（addOverlapBetweenParents）：相邻父块注入 ~12%（PARENT_OVERLAP_RATIO）句子级重叠
  │   └─ 取自原文快照、限制 30~200 token，不跨父块级联累积
  │
  └─ 8. 构建父子结构（buildParentChildDocuments）
      ├─ 子块大小 childSize = max(MIN_CHILD_TOKENS=64, chunkSize * CHILD_SIZE_RATIO=1/3)
      ├─ 子块间注入 ~15%（CHILD_OVERLAP_RATIO）尾部句子重叠
      ├─ 子块 metadata：parent_id（`{docId}-parent-{p}` 全局唯一）、parent_text（父块全文）、
      │   chunk_index、child_index、start_index、heading
      └─ 极端兜底：单个子块 > childSize*2 时走 TokenTextSplitter 细切
```

**关键参数**：

| 参数 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `MAX_WINDOW_SIZE` | `2` | 多尺度滑动窗口最大半径（实际窗口按单元数自适应收缩） |
| `THRESHOLD_STRATEGY` | `STANDARD_DEVIATION` | 动态阈值策略：PERCENTILE / STANDARD_DEVIATION / INTERQUARTILE / GRADIENT |
| `THRESHOLD_AMOUNT` | `1.5` | 标准差/四分位法的 k 值（阈值 = mean - k*std 或 Q1 - k*IQR） |
| `PERCENTILE_FRACTION` | `0.20` | PERCENTILE 策略的切分分位（相似度最低的 20%） |
| `LOCAL_MIN_RADIUS` | `1` | 局部极小值校验半径，剔除相似度窄幅抖动噪声 |
| `MIN_UNIT_TOKENS` | `100` | 语义单元/块最小 Token 数（≈250 中文字符），过小则合并 |
| `MAX_UNIT_TOKENS_FACTOR` | `2` | 单元/父组上限 = chunkSize * 2，超过则二次拆分 |
| `CHILD_SIZE_RATIO` | `1/3` | 子块大小 = chunkSize * 1/3（不低于 MIN_CHILD_TOKENS） |
| `MIN_CHILD_TOKENS` | `64` | 子块最小 Token 数 |
| `CHILD_OVERLAP_RATIO` | `0.15` | 子块间尾部句子重叠比例（~15%） |
| `PARENT_OVERLAP_RATIO` | `0.12` | 相邻父块句子级重叠比例（~12%，限 30~200 token） |
| `EMBEDDING_BATCH_SIZE` | `16` | Embedding 分批调用大小 |
| `embeddingMaxInputTokens` | `512` | Embedding 模型单次输入上限（`aichat.embedding.max-input-tokens`） |
| `REMOVE_URL_AND_EMAIL` | `true` | 清洗阶段删除 URL / 邮箱 |
| `TOKEN_CACHE_MAX_ENTRIES` | `10000` | 短文本 Token 估算 LRU 缓存容量 |

**父子分块（Parent-Child Chunking）**：

- **子块检索、父块回溯**：向量库只存**子块**（粒度小、语义聚焦，检索命中率高）；命中后直接读取子块 metadata 的 `parent_text`（父块全文）作为 LLM 上下文，无需二次查询。
- **parent_id 全局唯一**：格式 `{docId}-parent-{p}`，`docId` 优先取 metadata `doc_id`、缺失则随机 UUID；检索端可按 `parent_id` 去重，避免同一父块的多个子块重复占用上下文窗口。
- **双层重叠**：父块间 ~12% + 子块间 ~15% 的句子级重叠，缓解切分边界处的信息割裂。
- **对齐 Dify 父子模式**：兼顾“检索精度（小子块）”与“上下文完整性（大父块）”。

**降级机制**：

| 场景 | 降级策略 |
| :--- | :--- |
| 未配置 `EmbeddingModel` | `supports()` 返回 false，ChunkerFactory 自动选择 SeparatorChunker/TokenChunker |
| 语义单元数不足（无法形成有效窗口） | 降级为 Token 分块 |
| Embedding 计算异常 / 返回向量数与请求片段数不符 | 捕获并降级为 Token 分块 |
| 单个子块 > childSize*2（单句超长等极端情况） | 该子块走 TokenTextSplitter 细切 |

> **V5 变更**：不再因“语义断点样本数不足（旧 `MIN_GAP_SAMPLES`）”而整体降级为 Token 分块——语义断点稀少时改由 `splitOversizedGroups`（超长按单元边界拆）与 `mergeSmallGroups`（过小就近合并）兜底，保证父块粒度稳定；度量单位由字符数（旧 `MIN_UNIT_CHARS=250`）统一切换为 Token（`MIN_UNIT_TOKENS=100`，jtokkit CL100K_BASE）。

**三种策略对比**：

| 维度 | TokenChunker | SeparatorChunker | SemanticChunker |
| :--- | :--- | :--- | :--- |
| 切分依据 | Token 数量 + 标点 | 用户自定义正则 | Embedding 语义相似度（多策略动态阈值） |
| 结构感知 | 无 | 强（依赖分隔符设计） | 强（标题 → 段落 → 句子层级切分） |
| 语义完整性 | 可能切断语义边界 | 依赖分隔符合理性 | 语义相近单元自动聚合为父块 |
| 父子结构 | 无 | 无 | 有（子块检索 + 父块回溯，对齐 Dify） |
| 块间重叠 | 无 | 无 | 有（父块 ~12% + 子块 ~15%） |
| 计算开销 | 低（纯文本处理） | 低（正则 + Token 估算） | 高（需调用 Embedding 模型） |
| 适用场景 | 结构不明的通用文档 | Markdown / 代码 / FAQ / 日志 | 语义连贯、段落边界模糊的长文档 |
| 检索精度 | 依赖固定切分质量 | 高（结构对齐业务语义） | 最高（小子块命中 + 大父块上下文） |
| 阈值策略 | 无（固定 chunkSize） | 无（依赖分隔符） | 多策略动态阈值（自适应文档特征） |

### 2.7 SSE 流式推送

#### 2.7.1 双通道架构

```
ChatAgent.chatStream()
  │
  ├─ 主流：LLM 文本流 → chunk 事件 → 前端
  │
  └─ 旁路：HistoryChatMemoryAdvisor 拦截工具调用
           → tool_call 事件 → AgentEventSinkManager → 前端
  │
  └─ 旁路：RagContextQueryAdvisor 推送 RAG 检索状态
           → rag_retrieve 事件 → AgentEventSinkManager → 前端
  │
  └─ Flux.merge(主流, 旁路) → 统一 SSE 输出
```

#### 2.7.2 ChatStreamEvent 事件结构

| 事件类型 | 说明 | 触发时机 |
| :--- | :--- | :--- |
| `message` | LLM 增量文本 | 流式输出每个 chunk |
| `tool_call` | 工具调用通知 | LLM 发起工具调用时 |
| `rag_retrieve` | RAG 检索状态（start / success / empty） | `RagContextQueryAdvisor` 检索知识库时 |
| `message_end` | 消息结束 | 流式完成 |
| `error` | 错误 | 异常时 |

> **SSE 协议增强**：`tool_call` 与 `rag_retrieve` 事件现在显式设置 SSE `event` 头（`event: tool_call` / `event: rag_retrieve`），data 为 JSON 载荷（含 `event`、`content`、`conversationId` 字段）。前端按事件类型路由后解析 JSON 取 `content` 字段，非 JSON 载荷回退纯文本。

#### 2.7.3 AgentEventSinkManager

- 每个会话维护一个独立的 `Sinks.Many` 通道
- `ConcurrentHashMap` 管理所有会话的 Sink
- 工具调用时通过 `emitThought()` 推送旁路事件（带 `event: tool_call` 头）
- RAG 检索时通过 `emitRagRetrieve()` 推送状态事件（带 `event: rag_retrieve` 头）
- 流结束时 `complete()` 清理通道

### 2.8 工具调用编排

#### 2.8.1 工具调用全链路

```
LLM 决定调用工具
  │
  ├─ ToolSearchToolCallingAdvisor 动态检索相关工具
  │   └─ 从 ToolIndex (Lucene/VectorStore) 搜索匹配工具
  │
  ├─ WrappedMcpToolCallbackProvider 获取工具回调
  │   └─ 用 ReturnDirectToolCallbackWrapper 包装每个工具
  │
  ├─ ReturnDirectToolCallbackWrapper.call() 执行
  │   ├─ 【输入拦截】检测 dataId:xxx → 从 Redis 取真实数据替换
  │   ├─ 【工具执行】通过 MCP 协议调用 mall-ai-mcp-server
  │   └─ 【输出拦截】结果 > 2000字符 → 存入 Redis，返回 dataId:xxx
  │
  └─ 结果返回 LLM（大数据被替换为 dataId 引用，节省 Token）
```

#### 2.8.2 ToolSearch 工具动态检索流程

**核心问题**：系统注册了大量 MCP 工具（部门 CRUD、用户 CRUD、角色 CRUD、NL2SQL、ECharts 等），如果每次请求都把所有工具定义传给 LLM，会导致：
- **Token 浪费**：几十个工具的 JSON Schema 占用大量上下文
- **模型幻觉**：工具过多时 LLM 容易选错工具或编造参数
- **响应变慢**：Prompt 过长导致推理延迟增加

**解决方案**：使用 Spring AI 2.0 的 `ToolSearchToolCallingAdvisor`（本项目采用其增强子类 **`HistoryAwareToolSearchAdvisor`**），采用 **"渐进式工具披露"（Progressive Tool Disclosure）** 模式——初始只给 LLM 一个内置的 `toolSearchTool`，由 LLM 按需主动搜索并发现业务工具。

##### 配置

```yaml
spring:
  ai:
    chat:
      client:
        tool-search-advisor:
          enabled: true                    # 启用工具搜索
          tool-index-type: vector          # 索引类型：lucene（本地）或 vector（向量库）
          max-results: 3                   # 每次搜索最多返回 3 个工具
```

##### ToolIndex 两种实现（通过 `vectorstore.enabled` 自动切换）

| 方案 | 实现类 | 触发条件 | 说明 |
| :--- | :--- | :--- | :--- |
| 方案 A | `VectorToolIndex` | `vectorstore.enabled=true` | 基于 Weaviate 向量库（`toolVectorStore`），语义检索工具 |
| 方案 B | `LuceneToolIndex(0.3f)` | `vectorstore.enabled=false`（默认） | 本地 Lucene 倒排索引，相似度阈值 0.3 |

两种实现通过 `@ConditionalOnProperty` 切换，Bean 名称统一为 `toolIndex`，调用方无需感知差异。

##### 完整流程（基于 Spring AI 2.0 源码分析）

`ToolSearchToolCallingAdvisor` 是一个**递归 Advisor**（Recursive Advisor），会多次进入下游链路。核心机制是**工具的按需发现**，而非预先注入：

```
┌─────────────────────────────────────────────────────────────────┐
│ 阶段 0：会话启动（Indexing Phase）                                │
│                                                                 │
│  ToolSearchToolCallingAdvisor 启动时：                            │
│  ├─ 收集所有已注册的工具（通过 WrappedMcpToolCallbackProvider      │
│  │   从 MCP Gateway 发现的全部工具）                               │
│  ├─ 将每个工具的 name + description + inputSchema 索引到 ToolIndex │
│  │   ├─ LuceneToolIndex: 构建本地倒排索引                          │
│  │   └─ VectorToolIndex: 向量化后存入 Weaviate                    │
│  └─ 【关键】不将任何业务工具定义注入到 Prompt 中                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 阶段 1：初始请求（Initial Request）                               │
│                                                                 │
│  用户发送消息："帮我查询张三的部门信息"                              │
│  ├─ 请求进入 Advisor 链                                          │
│  ├─ ToolSearchToolCallingAdvisor 拦截（before 阶段）              │
│  └─ 【关键】只注入一个内置工具 toolSearchTool 到请求的 tools 中    │
│                                                                 │
│  LLM 收到的请求：                                                 │
│  ├─ 用户消息："帮我查询张三的部门信息"                              │
│  ├─ system prompt（含 toolSearchTool 的使用说明）                 │
│  └─ tools: [toolSearchTool]  ← 只有这一个工具！                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 阶段 2：LLM 发起工具搜索（Discovery Call）                        │
│                                                                 │
│  LLM 分析用户问题，判断需要调用工具                                 │
│  ├─ 但 LLM 此时只看到 toolSearchTool，不知道有哪些业务工具          │
│  └─ LLM 决定调用 toolSearchTool，传入查询关键词：                  │
│                                                                 │
│  toolSearchTool({ "query": "查询用户 部门 信息" })                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 阶段 3：搜索并扩展（Search & Expand）                             │
│                                                                 │
│  ToolSearchToolCallingAdvisor 拦截 toolSearchTool 调用：           │
│  ├─ 调用 ToolIndex.search(query, maxResults=3)                   │
│  │   ├─ LuceneToolIndex: 关键词匹配 + 相似度评分（阈值 0.3）       │
│  │   └─ VectorToolIndex: 向量相似度检索（Weaviate）               │
│  ├─ 返回 top 3 个匹配工具的定义（name + description + schema）    │
│  │   例：userCrud、deptCrud、getMenuComponent                     │
│  └─ 将这些工具定义动态扩展到下一轮请求的 tools 参数中               │
│                                                                 │
│  下一轮请求 LLM 收到的 tools：                                    │
│  [toolSearchTool, userCrud, deptCrud, getMenuComponent]          │
│  ← 搜索到的 3 个 + 原有的 toolSearchTool                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 阶段 4：LLM 调用实际工具（Tool Invocation）                       │
│                                                                 │
│  LLM 现在看到了具体工具定义，决定调用：                             │
│  ├─ 调用 userCrud({ "operation": "query", "userName": "张三" })  │
│  │   → 返回张三的用户信息（含部门关联）                            │
│  │                                                               │
│  ├─ 如需更多信息，可继续调用 toolSearchTool 搜索其他工具           │
│  │   或直接调用已发现的 deptCrud                                   │
│  │                                                               │
│  └─ 工具执行通过 WrappedMcpToolCallbackProvider → MCP 协议        │
│     → mall-ai-mcp-gateway → mall-ai-mcp-server                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 阶段 5：循环或终止                                                │
│                                                                 │
│  ToolSearchToolCallingAdvisor 检查 LLM 响应：                     │
│  ├─ 若包含工具调用 → 执行工具，将结果加入对话历史，重新进入阶段 2   │
│  └─ 若无工具调用 → 返回最终回复，结束递归循环                      │
│                                                                 │
│  最终回复："张三属于研发部，部门负责人是李四..."                    │
└─────────────────────────────────────────────────────────────────┘
```

**关键设计要点**（源码分析结论）：

1. **初始零工具**：会话启动时业务工具全部被索引但不注入，LLM 初始只能看到 `toolSearchTool` 一个工具
2. **LLM 驱动发现**：由 LLM 主动判断"需要工具"并调用 `toolSearchTool`，而非框架自动注入
3. **递归循环**：ToolSearchToolCallingAdvisor 是递归 Advisor，会多次进入下游链路，直到 LLM 不再发起工具调用
4. **渐进扩展**：每轮搜索到的工具定义会累积保留，LLM 可在一轮中多次搜索不同关键词
5. **保留搜索能力**：即使已发现工具，`toolSearchTool` 始终可用，LLM 可随时搜索更多工具

##### toolSearchTool 内置工具定义

`ToolSearchToolCallingAdvisor` 在初始化时自动注册一个名为 `toolSearchTool` 的内置工具，其定义如下：

| 属性 | 值 |
| :--- | :--- |
| 工具名 | `toolSearchTool` |
| 描述 | 引导 LLM 在需要工具能力时主动搜索，而非猜测工具名 |
| 输入参数 | `query`（String）：搜索关键词，描述所需工具的能力 |
| 返回值 | 匹配工具的定义列表（name + description + inputSchema） |

**LLM 看到的 Schema 示例**：
```json
{
  "name": "toolSearchTool",
  "description": "Search for available tools by capability description...",
  "inputSchema": {
    "type": "object",
    "properties": {
      "query": {
        "type": "string",
        "description": "Natural language description of the tool capability you need"
      }
    },
    "required": ["query"]
  }
}
```

**工作机制**：LLM 分析用户问题后，若判断需要外部能力，会构造一个查询字符串调用此工具。框架拦截该调用，在 ToolIndex 中执行搜索，将匹配的工具定义作为工具响应返回给 LLM，LLM 随后即可调用已发现的工具。

##### 搜索结果处理机制（双通道传递）

ToolSearchToolCallingAdvisor 查询到匹配工具后，通过**两个通道**将工具信息传递给 LLM：

```
LLM 调用 toolSearchTool({ "query": "查询用户部门信息" })
  │
  ├─ ToolSearchToolCallingAdvisor 拦截调用
  │   └─ ToolIndex.search(query, maxResults=3)
  │       └─ 返回匹配工具列表：[userCrud, deptCrud, getMenuComponent]
  │
  ├─ 【通道 1】ToolResponseMessage（对话历史）
  │   ├─ 将搜索结果序列化为 JSON 字符串，作为 toolSearchTool 的返回值
  │   ├─ 构造 ToolResponseMessage，添加到对话历史
  │   └─ LLM 在下一轮请求的对话历史中看到：
  │       "[{"name":"userCrud","description":"用户增删改查..."},
  │         {"name":"deptCrud","description":"部门增删改查..."},
  │         {"name":"getMenuComponent","description":"获取菜单组件..."}]"
  │
  └─ 【通道 2】Tool Definitions（tools 参数）
      ├─ Advisor 内部维护"已发现工具"集合（Set<ToolCallback>）
      ├─ 将搜索到的工具定义添加到该集合
      └─ 下一轮请求构造时，tools 参数 = toolSearchTool + 已发现工具的完整 JSON Schema
          → LLM 不仅能看到工具描述（通道1），还能看到完整参数定义（通道2）
          → 因此 LLM 可以实际调用这些工具
```

**两通道的职责区分**：

| 通道 | 内容 | 作用 | LLM 如何感知 |
| :--- | :--- | :--- | :--- |
| **ToolResponseMessage** | 工具的 name + description（精简信息） | 告知 LLM "有哪些工具可用" | 作为对话历史的一部分，LLM 可读取 |
| **Tool Definitions** | 工具的完整 JSON Schema（含参数定义） | 使 LLM "能够调用这些工具" | 作为请求的 tools 参数，LLM 可直接调用 |

**为什么需要双通道**：
- 仅有通道 1：LLM 知道有 `userCrud` 这个工具，但不知道参数格式，无法调用
- 仅有通道 2：LLM 能看到工具定义，但不知道为什么这些工具被选中（缺少上下文）
- 双通道配合：LLM 既理解上下文（通道 1），又能实际调用（通道 2）

**迭代过程中的工具累积**：

```
第 1 轮请求：tools = [toolSearchTool]
  ↓ LLM 调用 toolSearchTool("查询用户")
第 2 轮请求：tools = [toolSearchTool, userCrud]
                                    ↑ 新发现
  ↓ LLM 调用 toolSearchTool("查询部门")
第 3 轮请求：tools = [toolSearchTool, userCrud, deptCrud]
                                              ↑ 新发现
  ↓ LLM 调用 userCrud({ "operation": "query", "userName": "张三" })
第 4 轮请求：tools = [toolSearchTool, userCrud, deptCrud]（不变，工具已全部发现）
  ↓ LLM 不再调用工具，返回最终回复
```

**关键点**：
- 已发现的工具会**累积保留**在 tools 参数中，不会因为新一轮搜索而丢失
- `toolSearchTool` **始终存在**于 tools 参数中，LLM 可随时搜索更多工具
- 每轮请求的对话历史会包含所有之前的工具调用记录（查询 + 结果）

##### 三种搜索策略对比

Spring AI 2.0 提供 3 种 `ToolIndex` 实现（本项目使用前两种）：

| 策略 | 实现类 | 原理 | 适用场景 | 本项目配置 |
| :--- | :--- | :--- | :--- | :--- |
| **语义检索** | `VectorToolIndex` | 将工具描述向量化，通过 Embedding 相似度匹配 | 自然语言查询、模糊匹配、工具描述较长 | `vectorstore.enabled=true` 时启用 |
| **关键词检索** | `LuceneToolIndex` | Apache Lucene 倒排索引，TF-IDF 评分 | 精确术语匹配、已知工具名、工具名有规律 | `vectorstore.enabled=false` 时启用（默认），阈值 0.3 |
| **正则匹配** | `RegexToolIndex` | 正则表达式匹配工具名（如 `get_*_data`） | 工具名有明确命名模式 | 未使用 |

**本项目选择逻辑**：
- 默认使用 `LuceneToolIndex(0.3f)`：工具数量适中（10+），关键词匹配足够精准，无需额外向量库开销
- 开启向量库时切换到 `VectorToolIndex`：工具数量大幅增长后，语义检索优势更明显

##### 性能基准（Spring AI 官方数据）

基于 28 个工具的测试，Token 消耗对比：

| LLM 模型 | 有 Tool Search | 无 Tool Search | Token 节省 |
| :--- | :--- | :--- | :--- |
| Gemini | 2,165 | 5,375 | **60%** |
| OpenAI | 4,706 | 7,175 | **34%** |
| Anthropic | 6,273 | 17,342 | **64%** |

**结论**：工具越多，ToolSearch 的 Token 节省效果越显著。本项目使用通义千问（OpenAI 兼容协议），预计节省 30-40%。

##### 递归 Advisor 机制

`ToolSearchToolCallingAdvisor` 继承自 `ToolCallingAdvisor`，是 Spring AI 2.0 的**递归 Advisor**（Recursive Advisor）：

```
用户请求 → Advisor 链 → ToolSearchToolCallingAdvisor → 下游链 → LLM
                          ↑                              │
                          │     ┌─ 有工具调用？           │
                          │     │   是 → 执行工具 ────────┘
                          └─────┘   否 → 返回最终结果
```

**循环条件**：LLM 响应中包含工具调用请求（`finishReason == "tool_calls"`）
**终止条件**：LLM 响应中不包含工具调用（`finishReason == "stop"`）

**每轮迭代的上下文累积**：
- 第 1 轮：用户消息 + toolSearchTool
- 第 2 轮：用户消息 + toolSearchTool + toolSearchTool 调用结果（匹配的工具定义）
- 第 3 轮：上述 + 实际工具调用结果
- ...直到 LLM 不再调用工具

##### 何时使用 ToolSearch

| 适用场景 | 不适用场景 |
| :--- | :--- |
| 系统中有 20+ 个工具 | 工具数量少（<20 个） |
| 工具定义消耗 >5K tokens | 每次会话都用所有工具 |
| 构建多 MCP Server 系统 | 工具定义非常精简 |
| 遇到工具选择准确性问题 | |

**本项目情况**：已注册 10+ 个 MCP 工具（7 个 CRUD + NL2SQL + ECharts + 业务聚合），且通过 MCP Gateway 可动态扩展，符合使用 ToolSearch 的条件。

##### Advisor 链中的位置

| 顺序 | Advisor | Order | 职责 |
| :--- | :--- | :--- | :--- |
| 1 | VectorStoreChatMemoryAdvisor | 98 | 长期语义记忆 |
| 2 | ReturnDirectChatMemoryAdvisor | 99 | returnDirect 工具结果拦截 |
| **3** | **HistoryAwareToolSearchAdvisor** | **100** | **工具动态检索注入** |
| 4 | SessionMemoryAdvisor | 101 | 近期上下文窗口记忆 |
| 5 | RagContextQueryAdvisor | 102 | 知识库上下文查询 |
| 6 | HistoryChatMemoryAdvisor | 103 | 全量记录 + 工具审计 + tool_call 事件推送 |
| 7 | SimpleLoggerAdvisor | 104 | 请求/响应日志 |

**顺序设计要点**：
- 工具搜索 Advisor order=100，通过 `HistoryAwareToolSearchAdvisor.newBuilder().advisorOrder(100)` 显式指定
- 位于记忆 Advisor 之后：确保记忆已加载完成再搜索工具
- 位于历史记录 Advisor（103）之前：确保 `HistoryChatMemoryAdvisor` 能拿到最终的工具调用信息写入审计日志

##### HistoryAwareToolSearchAdvisor（工具回调丢失修复）

`ToolSearchToolCallingAdvisor` 的“渐进式工具披露”在**跨轮/历史压缩/多迭代**场景下存在缺陷：父类 `prepareIteration` 在**每一轮迭代**都会用重新计算的 `selectedToolCallbacks`（仅 `toolSearchTool` + 当前消息窗口内发现的工具）**整体覆盖** `options.toolCallbacks`。当会话历史被压缩或跨轮请求时，最初的搜索发现响应可能已不在窗口内，但 LLM 仍会从持久化历史里看到自己之前调用过某工具，于是直接复调——此时回调集合里没有它，报 `No ToolCallback found for tool name`。

`HistoryAwareToolSearchAdvisor` 采用**双层防护**修复：

| 防护层 | 实现 | 作用 |
| :--- | :--- | :--- |
| **Prompt 侧** | `restoreHistoryReferencedTools()`（每轮 `doBeforeCall`/`doBeforeStream` 在父类覆盖之后执行） | 扫描历史 Assistant 消息里调用过的工具，把“当前回调集合缺失但原始集合存在”的工具定义补回，保证模型能合法看到并复调 |
| **执行侧** | `HistoryAwareToolCallingManager`（装饰传给父类的 `ToolCallingManager`） | 无论父类内部循环覆盖多少次，真正 `executeToolCalls` 前依据 sessionId 从会话级全量注册表兜底补全回调 |

**关键机制**：
- `captureFullToolCallbacks()`：每轮在父类覆盖前捕获原始全量工具回调（name → callback），写入 context 并按 `sessionId` 注册到装饰 manager
- `HistoryAwareToolCallingManager.augment()`：仅补“当前缺失”项，不整体塞回，保留渐进式披露的 token 优势
- `evictSession()`：会话结束时 `unregister(sessionId)` 清理注册表，避免内存泄漏
- 两层配合下：prompt 里默认仍只暴露已发现工具（保留 token 优势），而执行侧永远不缺 callback

##### 关键类

| 类/Bean | 职责 |
| :--- | :--- |
| `HistoryAwareToolSearchAdvisor` | **自定义**：继承 `ToolSearchToolCallingAdvisor`，双层防护修复跨轮/压缩丢失工具回调 |
| `HistoryAwareToolCallingManager` | **自定义**：执行期兜底装饰器，按 sessionId 从全量注册表补回缺失回调 |
| `ToolSearchToolCallingAdvisor` | 递归 Advisor，索引工具 + 注入 toolSearchTool + 拦截搜索调用 |
| `ToolIndex` | 工具索引接口（`search(query, maxResults)`） |
| `LuceneToolIndex` | Lucene 实现，本地倒排索引，阈值 0.3 |
| `VectorToolIndex` | 向量库实现，基于 Weaviate 语义检索 |
| `RegexToolIndex` | 正则实现，按工具名模式匹配（本项目未使用） |
| `ChatClientConfig` | 配置类，注册 ToolIndex 和 HistoryAwareToolSearchAdvisor Bean |

##### 效果对比

| 指标 | 无 ToolSearch | 有 ToolSearch |
| :--- | :--- | :--- |
| LLM 看到的工具数 | 全部（10+ 个） | 初始 1 个（toolSearchTool），按需扩展 |
| Prompt Token 消耗 | 高（所有工具 Schema） | 低（仅 toolSearchTool + 搜索结果） |
| 工具选择准确性 | 易混淆 | LLM 主动搜索，精准匹配 |
| 新增工具影响 | 需重启，Token 持续增长 | 自动索引，影响小 |
| LLM 交互轮数 | 1 轮（直接调用） | 2+ 轮（先搜索再调用） |

#### 2.8.3 工具大数据缓存（单体编排层模式）

**问题**：nl2sql 工具返回 5000 字符的查询结果，直接传给 LLM 会撑爆上下文。

**解决方案**：

| 阶段 | 操作 |
| :--- | :--- |
| 工具输出 > 2000字符 | 存入 Redis（`mall:ai:tool:data:{dataId}`，TTL 1小时），返回 `dataId:xxx` |
| 下一个工具输入含 `dataId:xxx` | 从 Redis 取真实数据，用 `JSON.toJSONString` 安全转义后替换 |

**配置项**：
```yaml
ai:
  tool:
    cache:
      threshold: 2000       # 缓存阈值
      ttl-hours: 1          # 过期时间
```

**核心类**：`ToolDataCacheService`、`ReturnDirectToolCallbackWrapper`

#### 2.8.4 returnDirect 机制

工具 description 以 `[JSON]` 结尾时，`ReturnDirectToolCallbackWrapper` 自动设置 `returnDirect=true`。

**效果**：工具结果不经过 LLM 处理，直接通过 SSE 返回前端。适用于图表配置等结构化数据。

---

## 3. MCP 工具服务 (mall-ai-mcp-server)

### 3.1 目录结构

```
com.mall.chatmcp
├── config/
│   └── McpServerConfig.java            # MCP Server 配置 + ChatClient Bean
├── bo/
│   # 主实体 BO（CRUD 参数）
│   ├── SysUserBo.java                  # 用户工具参数
│   ├── SysDeptBo.java                  # 部门工具参数
│   ├── SysRoleBo.java                  # 角色工具参数
│   ├── SysPostBo.java                  # 岗位工具参数
│   ├── SysNoticeBo.java                # 公告工具参数
│   ├── SysConfigBo.java                # 参数配置工具参数
│   ├── SysDictTypeBo.java              # 字典类型工具参数
│   ├── SysDictDataBo.java              # 字典数据工具参数
│   └── SysJobBo.java                   # 定时任务工具参数
│   # 关系型 BO（权限/关联绑定参数，均按名称传参，不含 ID）
│   ├── UserRoleBo.java                 # 用户-角色绑定（userName + roleNames[]）
│   ├── UserDeptBo.java                 # 用户-部门绑定（userName + deptName）
│   ├── UserPostBo.java                 # 用户-岗位绑定（userName + postNames[]）
│   ├── RoleMenuBo.java                 # 角色-菜单权限（roleName + menuNames[]）
│   └── RoleDeptBo.java                 # 角色-数据权限（roleName + deptNames[]）
└── sevice/
    ├── BaseToolService.java            # 工具接口
    └── impl/
        ├── BaseToolServiceImpl.java    # 抽象基类（校验/异常/日志）
        ├── DeptToolServiceImpl.java    # 部门 CRUD
        ├── UserToolServiceImpl.java    # 用户 CRUD + 状态管理 + 密码重置
        ├── RoleToolServiceImpl.java    # 角色 CRUD
        ├── NoticePostToolServiceImpl.java # 公告+岗位 CRUD
        ├── OpenMenuToolServiceImpl.java   # 菜单导航
        ├── DeptBizToolServiceImpl.java    # 部门业务聚合工具
        ├── Nl2SqlToolServiceImpl.java     # 自然语言转 SQL
        ├── ConfigToolServiceImpl.java     # 系统参数配置 CRUD + 查询
        ├── DictToolServiceImpl.java       # 字典类型/字典数据 CRUD + 查询
        ├── JobToolServiceImpl.java        # 定时任务 CRUD + 状态修改 + 立即执行 + 日志查询
        └── LogToolServiceImpl.java        # 操作日志/登录日志查询 + 登录失败统计
```

### 3.2 工具基类设计（模板方法模式）

`BaseToolServiceImpl` 封装公共逻辑，所有工具类继承该基类：

| 方法 | 职责 |
| :--- | :--- |
| `validate(obj, objName)` | JSR-303 参数校验，失败返回 AjaxResult.error |
| `executeWithErrorHandling(callback, desc)` | 统一异常处理 + 耗时日志 |
| `logOperation(type, name, result)` | 操作日志记录 |

### 3.3 工具清单

| 工具类 | 工具方法 | 说明 |
| :--- | :--- | :--- |
| `DeptToolServiceImpl` | `deptCrud` | 部门增删改查，按名称查询而非 ID |
| `UserToolServiceImpl` | `userCrud` | 用户增删改查 + 状态管理 |
| `UserToolServiceImpl` | `resetUserPassword` | 重置用户密码（按用户名查找） |
| `UserToolServiceImpl` | `changeUserStatus` | 修改用户状态（启用/停用） |
| `RoleToolServiceImpl` | `roleCrud` | 角色增删改查 |
| `NoticePostToolServiceImpl` | `noticeCrud` / `postCrud` | 公告/岗位增删改查 |
| `OpenMenuToolServiceImpl` | `getMenuComponent` | 菜单导航 |
| `DeptBizToolServiceImpl` | `createDeptWithAdmin` / `batchCreateDepts` | 业务聚合工具 |
| `Nl2SqlToolServiceImpl` | `nl2SqlQuery` | 自然语言转 SQL 查询 |
| `ConfigToolServiceImpl` | `configCrud` / `configQuery` | 系统参数配置增删改 + 查询 |
| `DictToolServiceImpl` | `dictTypeCrud` / `dictTypeQuery` | 字典类型增删改 + 查询 |
| `DictToolServiceImpl` | `dictDataCrud` / `dictDataQuery` | 字典数据增删改 + 查询 |
| `JobToolServiceImpl` | `jobCrud` / `jobQuery` / `jobLogQuery` | 定时任务增删改 + 状态修改 + 立即执行 + 日志查询 |
| `LogToolServiceImpl` | `operLogQuery` | 操作日志查询（按操作人/模块/状态） |
| `LogToolServiceImpl` | `loginLogQuery` | 登录日志查询（按用户名/状态） |
| `LogToolServiceImpl` | `loginFailStats` | 统计登录失败次数最多的用户 |

### 3.4 工具设计规范

#### 3.4.1 按名称查询替代 ID 直传

所有删除/修改操作**不接受 ID 参数**，改为按名称查询：

```
按名称查询 → 0条：报错"不存在"
           → 1条：用查询结果的ID执行操作
           → 多条：报错"请补充更多信息后重试"
```

多结果时支持组合条件查询（如 userName + nickName + phone + email）。

#### 3.4.2 不暴露内部 ID

`formatXxxList` 方法返回给前端的信息中不包含 ID，改为展示用户可识别的字段：

| 实体 | 展示字段 |
| :--- | :--- |
| 用户 | 用户名、昵称、手机、邮箱 |
| 部门 | 部门名称、负责人、联系电话 |
| 角色 | 角色名称、权限字符、状态 |
| 岗位 | 岗位名称、岗位编码、状态 |

#### 3.4.3 业务聚合工具

`DeptBizToolServiceImpl` 封装多步操作为单一工具：

| 方法 | 封装的步骤 |
| :--- | :--- |
| `createDeptWithAdmin` | 创建部门 → 查询用户 → 分配部门 → 创建角色 → 分配角色 |
| `batchCreateDepts` | 按顺序创建多个部门，支持父子关系 |

**收益**：减少模型需要理解的工具数量，核心流程锁死在代码里，不依赖模型乱拼。

### 3.5 NL2SQL 工具

**完整执行链路**（对齐 Alibaba NL2SQL 主干：召回 → 外键扩展 → LLM 精筛 → 语义层 → 生成 → 校验 → 执行 → 语义校验 → 自愈重试）：

```
用户消息（+ 可选 chatHistory 最近几轮对话）
  │
  ├─ 并行召回（CompletableFuture + 专用线程池，省 1~2 秒串行等待）
  │   ├─ Schema 召回（Feign 调用 chat 服务，kbType=20）
  │   │   ├─ 标签反向匹配找出相似 KbDocument → knowledgeId 单重过滤向量检索
  │   │   ├─ Reranker 重排序返回表结构 Schema
  │   │   └─ 外键扩展（FK Expansion）：读 information_schema.KEY_COLUMN_USAGE
  │   │       双向查询外键，补全关联表 DDL + 显式声明 JOIN 关系
  │   │       （相比 LLM 推理表关系更准更快）
  │   └─ 业务语义上下文（结构化直查为主，kbType=21 自由文本兜底）
  │       ├─ 指标定义：Feign 查 chat 内部 API，按 metric_name/synonyms 匹配
  │       ├─ 维度定义：Feign 查 chat 内部 API，按 dim_name/synonyms 匹配
  │       │   （含维度枚举值，提示 LLM 用值而非含义）
  │       ├─ 业务规则：Feign 查 chat 内部 API，applies_to 为空的全量 + 命中规则
  │       └─ 三层均未命中 → 降级 kbType=21 向量召回（长尾术语兜底）
  │
  ├─ LLM 精筛表数据（候选表数 ≥ 5 时触发）
  │   └─ 轻量 LLM 调用筛选出回答问题所必需的表，剔除无关表 DDL 噪声
  │
  ├─ LLM 意图分类 + 指代消解（结构化 JSON 三态输出）
  │   ├─ QUERY：需要查库 → 生成 SQL（rewritten 字段 = 指代消解后的完整问题）
  │   ├─ CHAT：闲聊/写作/打招呼 → 直接返回说明，不进 SQL 链路
  │   ├─ CLARIFY：歧义/缺条件 → 返回澄清问题
  │   └─ 时间感知：Prompt 注入数据库当前时间（SELECT NOW()，60s 缓存），
  │       "上个月/本周/近30天"等相对时间据此换算为具体日期范围
  │
  ├─ 多候选生成（CANDIDATE_COUNT=2）
  │   ├─ 同一次生成 2 个不同思路候选 + 自评分
  │   └─ 校验失败时优先用备选候选，不重新调用 LLM
  │
  ├─ 安全校验（双层防线）
  │   ├─ 表名白名单校验（防止 LLM 幻觉出不存在的表）
  │   └─ JSqlParser AST 校验（只允许单条 SELECT，拒绝注入/多语句）
  │
  ├─ LIMIT 保护（自动添加 LIMIT 100；全 AST 检测聚合函数——含子查询/
  │   标量子查询/HAVING/UNION，修复顶层检测漏判导致的误加 LIMIT）
  │
  ├─ 低基数字段枚举值注入（DDL 构建时）
  │   ├─ 列注释含"状态/类型/是否/标志/标记" 或 char/varchar 且长度 ≤ 10 → 疑似枚举列
  │   ├─ DISTINCT 取值（LIMIT 10，表级 TTL 1 小时缓存）追加到 DDL 注释
  │   └─ 解决语义表未覆盖的枚举列"查停用用户查空"类问题
  │
  ├─ 远程执行（Feign 调用 mall-system）+ 结果值格式化
  │   ├─ Timestamp → yyyy-MM-dd HH:mm:ss，BigDecimal → toPlainString
  │   │   （避免科学计数法），byte[] → 占位文本（LLM 友好形态）
  │   └─ 结果达到 LIMIT 上限时在 summary 提示可能截断
  │
  ├─ 语义一致性校验（条件触发：多表 JOIN / 空结果 / 聚合意图）
  │   ├─ 6 维聚焦检查（指标/维度/时间/过滤条件/聚合方式/排序，对齐 DataAgent）
  │   ├─ 独立低 temperature（可配独立模型），不暴露生成侧 reasoning，
  │   │   降低同模型同参数的相关性误判风险
  │   └─ 判定不一致 → 携带反馈进入自愈重试
  │
  └─ 自愈重试（统一重试循环，最多 2 次，全程受总时延预算约束）
      ├─ 语义失败重试：可选启用问题扩写后重新召回证据（默认关闭，时延敏感）
      └─ 预算耗尽 → 放弃重试返回明确错误（防止 MCP 调用方先超时）
```

**关键实现方法**（`Nl2SqlToolServiceImpl`）：

| 方法 | 职责 |
| :--- | :--- |
| `nl2SqlQuery()` | 工具入口：Schema/语义上下文并行召回 → 节点管道（Nl2SqlState 贯穿）；`chatHistory` 可选参数支持多轮指代消解 |
| `runPipeline()` | 编排化节点管道：每个节点 State → State，可单测；统一重试循环 |
| `nodeGenerate()` | 节点1：意图分类 + 指代消解 + 多候选生成；CLARIFY/CHAT 直接写 result 出口 |
| `nodeValidateExecute()` | 节点2：候选 SQL 校验+执行+语义校验（候选循环），任一成功写 result |
| `renderSemanticContext()` | 语义层结构化直查：指标/维度/业务规则三层 Feign 直查直用，不走向量；未命中降级 kbType=21 |
| `retrieveEvidence()` | 业务证据召回（kbType=21，指标口径/术语定义），失败优雅降级不阻断 |
| `filterSchemaByLLM()` | LLM 精筛表数据：候选表数 ≥ 5 时剔除无关表 DDL 噪声 |
| `expandWithForeignKeys()` | 外键扩展：读 information_schema 双向查外键，补全关联表 DDL + JOIN 声明 |
| `generateSql()` | LLM 意图分类三态解析（type/sql/rewritten/explanation/clarify/reply）+ 多候选，单次调用带超时 |
| `buildPrompt()` | Schema + 当前时间 + 对话上下文 + 语义上下文 + 修正指令 + 思维链 + JSON 输出契约 |
| `getDbNowText()` | 数据库当前时间（SELECT NOW()，60s 缓存，失败降级应用服务器时间） |
| `checkSemanticConsistency()` | 语义校验（6 维聚焦检查 + 业务证据，独立 options：低 temperature + 可选独立模型） |
| `hasAggregateInSql()` | 全 AST 聚合检测（AggregateFinder 覆盖子查询/标量子查询/HAVING/UNION） |
| `needSemanticCheck()` | 语义校验触发条件：AST 表计数 >1（含逗号连接多表）/ 空结果 / 聚合意图关键词 |
| `isLikelyEnumColumn()` / `getEnumValues()` | 低基数字段枚举值检测 + DISTINCT 取值（表级 TTL 缓存） |
| `formatResultValues()` | 结果值格式化：Timestamp/BigDecimal/byte[] 转为 LLM 友好形态 |
| `wrapResult()` | 封装结果：CLARIFY/CHAT/QUERY 三分支 + rewrittenQuestion + 截断提示 |
| `extractTableNames()` | 从 Schema 中提取 `CREATE TABLE` 表名，构成白名单 |

**安全校验双层防线**：

| 防线 | 说明 |
| :--- | :--- |
| 表名白名单 | 从 Schema 提取合法表名，SQL 引用了白名单外的表名 → 拒绝并触发 LLM 自我修正 |
| JSqlParser AST | 精确识别语句类型，只允许 SELECT，自动拒绝 INSERT/UPDATE/DELETE/DROP 等 |
| LIMIT 保护 | 自动添加 `LIMIT 100`（全 AST 聚合检测，含子查询），防止查询过载 |

**可调配置**（`nl2sql.*`，均有默认值，可在 Nacos 配置中心覆盖）：

| 配置项 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `nl2sql.time-budget-ms` | 90000 | 总时延预算（毫秒）：召回+生成+重试全流程共享，超时放弃重试直接报错 |
| `nl2sql.llm-timeout-seconds` | 30 | 单次 LLM 调用超时（秒），生成与语义校验调用均生效 |
| `nl2sql.generate.temperature` | 0.2 | SQL 生成侧 temperature（保证确定性，同时保留候选多样性） |
| `nl2sql.semantic-check.temperature` | 0.0 | 语义校验独立 temperature（与生成侧隔离，降低相关性误判） |
| `nl2sql.semantic-check.model` | 空 | 语义校验独立模型名（如 qwen-plus，空则复用生成模型） |
| `nl2sql.retry-expand-enabled` | false | 语义失败重试时是否启用问题扩写后重新召回证据（默认关闭，时延敏感） |
| `nl2sql.async-threads` | 4 | 异步召回专用线程池大小（daemon 线程，避免 Feign 丢失 RequestContext） |
| `nl2sql.eval.cron` | 0 0 3 * * ? | 评测集定时执行 cron（设为 "-" 禁用定时评测） |

> 配套调整：`spring.ai.mcp.server.request-timeout` 需大于总时延预算（默认已调为 60s），
> 避免 MCP 层先于时延预算超时中断长查询。

**评测体系**（`Nl2SqlEvalService` + `sql/nl2sql_eval.sql`）：

黄金评测集让 Prompt/Reranker/证据的每次调整有据可依（对齐 Alibaba/析言 GBI 的运营化度量）：

- **用例来源**：Feign 调用 chat 服务内部 API（`RemoteNl2sqlEvalService.list()`）获取启用的评测用例（替代原裸 SQL 查询 `nl2sql_eval` 表），运营可在管理端维护；未建表时降级内置 15 条默认集
- **断言方式**：意图类型（QUERY/CHAT/CLARIFY）+ SQL 关键特征片段（contains/not_contains）+ 最小行数，不比对 SQL 文本（写法太多）
- **触发方式**：每日 03:00 定时跑（`@Scheduled`，`nl2sql.eval.cron` 可调）或 MCP 工具 `nl2SqlEvalRun` 手动触发
- **输出**：准确率 + 逐条用例明细（PASS/FAIL + 失败原因），15 条约 1~3 分钟适合低峰期执行

#### 3.5.1 NL2SQL 语义层（指标 / 维度 / 业务规则）

对齐析言 GBI 语义层结构化设计，新增 `nl2sql_semantic.sql` 三张语义表，由 mall-ai-chat 提供 CRUD 管理接口（`Nl2sqlMetricController` / `Nl2sqlDimensionController` / `Nl2sqlBusinessRuleController`），mcp-server 通过 Feign 直查直用：

| 语义表 | 关键字段 | 用途 |
| :--- | :--- | :--- |
| `nl2sql_metric` | metric_name / metric_expr / agg_default / unit / synonyms | 指标定义：口径表达式 + 默认聚合 + 单位，按名称/同义词匹配注入 |
| `nl2sql_dimension` | dim_name / table_name / column_name / dim_values / synonyms | 维度定义：字段路径 + 枚举值，提示 LLM 用值而非含义 |
| `nl2sql_business_rule` | rule_name / rule_content / applies_to | 业务规则：applies_to 为空的全量注入 + 命中问题关键词的规则 |

**渲染策略**（`renderSemanticContext()`）：

- **结构化直查为主**：指标/维度/业务规则三层 Feign 直查直用，不走向量——结构化渲染比自由文本更难被 LLM 忽略，且管理端可维护（改口径不用重新向量化）
- **降级策略**：三层都未命中时降级走原有 kbType=21 向量召回（长尾术语兜底）；语义表不存在或查询异常时同样降级，不影响主流程
- **并行召回**：语义上下文与 Schema 召回通过 `CompletableFuture` 并行执行（专用线程池 `nl2sql-async`，daemon 线程），省 1~2 秒串行等待
- **RequestContext 传递**：调用线程先快照 `RequestContextHolder`（网关透传的 token/租户信息），再传入异步任务，避免 Feign 调用丢失上下文

### 3.6 MCP 协议配置

```yaml
spring:
  ai:
    mcp:
      server:
        name: mall-ai-mcp-server
        protocol: STREAMABLE          # Streamable HTTP 协议
        streamable-http:
          mcp-endpoint: /mcp          # MCP 端点
          keep-alive-interval: 30s
```

工具通过 `MethodToolCallbackProvider` 自动注册为 MCP 工具，**注册到 Nacos** 后由 `mall-ai-mcp-gateway` 聚合，mall-ai-chat 通过 MCP Client 连接 Gateway 统一调用。

### 3.7 MCP 网关服务 (mall-ai-mcp-gateway)

基于 **Spring AI Alibaba 的 `spring-ai-alibaba-starter-mcp-gateway`** 实现，是工具调用的统一入口。

#### 3.7.1 核心职责

| 职责 | 说明 |
| :--- | :--- |
| 工具聚合 | 通过 Nacos 服务发现，聚合多个 MCP Server 的工具，统一对外暴露 `/mcp` 端点 |
| 服务解耦 | mall-ai-chat 只需连接 Gateway 一个地址，无需感知后端有多少个 MCP Server |
| 动态扩展 | 新增 MCP Server 只需注册到 Nacos，Gateway 自动发现并聚合，无需重启 |
| 工具列表查询 | 提供 `GET /api/gateway/tools` 接口，列出所有聚合的工具 |

#### 3.7.2 配置

```yaml
spring:
  ai:
    mcp:
      server:
        name: mall-ai-mcp-gateway
        protocol: STREAMABLE
        type: async
        streamable-http:
          mcp-endpoint: /mcp
          keep-alive-interval: 60s
    alibaba:
      mcp:
        gateway:
          enabled: true
          nacos:
            serviceNames:              # 配置要聚合的 MCP Server 服务名
              - mall-ai-mcp-server
          streamable:
            enabled: true
```

#### 3.7.3 mall-ai-chat 的 MCP Client 配置

mall-ai-chat 同时连接内部 Gateway 和外部 MCP Server：

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        type: ASYNC
        request-timeout: 600s          # MCP Client 请求超时（chat 侧）
        streamable-http:
          connections:
            gateway:                   # 内部 MCP 网关（聚合 mall-ai-mcp-server）
              url: http://localhost:9999
            mcp-echarts:               # 外部 ECharts MCP（自部署服务）
              url: http://114.132.102.8:2001
              endpoint: /mcp
```

#### 3.7.4 MCP 工具调用超时修复（NacosMcpGatewayToolCallback 覆盖类）

**问题**：`spring-ai-alibaba-mcp-gateway` 依赖的 `NacosMcpGatewayToolCallback` 存在两处**硬编码超时**，不受 `spring.ai.mcp.client.request-timeout` 控制，导致长耗时工具调用报 `Error: MCP call failed - java.util.concurrent.TimeoutException`：

| 场景 | 硬编码超时 | 影响 |
| :--- | :--- | :--- |
| MCP 协议工具（mcp-sse / mcp-streamable） | `McpClient.sync().build()` 未设 requestTimeout，SDK 默认仅 **20 秒** | 长查询（如 NL2SQL）直接超时 |
| HTTP/HTTPS 协议工具 | `getTimeoutDuration()` 硬编码 **30 秒** | 长 HTTP 调用超时 |

**修复方案**：沿用项目已有惯例（同包同名类覆盖依赖 jar，如 `WebClientStreamableHttpTransport`），新建覆盖类 `mall-ai-mcp-gateway/.../nacos/callback/NacosMcpGatewayToolCallback.java`：

1. `getTimeoutDuration()`：改为从配置读取 `mcp.gateway.tool-timeout-seconds`，**默认 600 秒**（原硬编码 30 秒）
2. `handleMcpStreamProtocol()`：`McpClient.sync(transport)` 增加 `.requestTimeout(timeout).initializationTimeout(timeout)`（原无超时设置，默认 20 秒）

**配置方式**（默认即生效，无需额外配置）：
```yaml
mcp:
  gateway:
    tool-timeout-seconds: 600   # 工具调用超时（秒），按需调整
```

> **注意**：升级 spring-ai-alibaba 版本时需同步检查此覆盖类与新版源码的兼容性（本地 1.1.2.2 版本同样硬编码 30s，暂无可配置的新版本）。

**关键类**：`MallAiMcpGatewayApplication`、`GatewayController`、`WebClientConfig`、`NacosMcpGatewayToolCallback`（覆盖类）

---

## 4. 向量库配置

### 4.1 三库隔离

`VectorStoreConfig` 定义了三个独立的 VectorStore，使用不同的 Weaviate ObjectClass：

| Bean 名称 | ObjectClass | 用途 | 过滤字段 |
| :--- | :--- | :--- | :--- |
| `conversationVectorStore` | ConversationHistory | 会话记忆（长期记忆） | userId（主）+ conversationId + status + expireAt |
| `knowledgeVectorStore` | KnowledgeBase | 知识库文档（RAG 检索） | knowledgeId |
| `toolVectorStore` | ToolIndex | 工具索引（Tool Search） | sessionId |

### 4.2 Schema 自动初始化

应用启动时通过 `ApplicationRunner` 自动检查并创建 Weaviate Schema：
- 距离算法：cosine
- 索引类型：hnsw
- 向量化器：none（向量由 Spring AI 计算后传入）

### 4.3 工具检索（Tool Search）

| 配置 | 值 | 说明 |
| :--- | :--- | :--- |
| `tool-index-type` | vector | 向量库语义检索（Weaviate toolVectorStore） |
| `max-results` | 3 | 每次搜索最多返回 3 个工具 |

**效果**：模型不一次性看到几十个工具定义，而是按需检索相关工具，减少 Token 消耗。

---

## 5. 关键类与组件清单

### 5.1 Advisor 链

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `VectorStoreChatMemoryAdvisor` | **自定义**：长期语义记忆（userId 跨会话检索 + 异步写入 + Mem0 两阶段决策） | order=98，实现 BaseChatMemoryAdvisor，调用 smallChatClient 提取/决策记忆 |
| `ReturnDirectChatMemoryAdvisor` | **自定义**：拦截 returnDirect 工具结果 | order=99，单独入库，不经过 LLM |
| `HistoryAwareToolSearchAdvisor` | **自定义**：继承 ToolSearchToolCallingAdvisor，双层防护修复跨轮/压缩丢失工具回调 | order=100 |
| `HistoryAwareToolCallingManager` | **自定义**：执行期兜底装饰器，按 sessionId 补回缺失工具回调 | 装饰父类 ToolCallingManager |
| `SessionMemoryAdvisor` | Spring AI 2.0 原生：近期上下文窗口记忆 + 滑动窗口压缩 | order=101，基于 SessionService（JDBC） |
| `RagContextQueryAdvisor` | **自定义**：知识库上下文查询（RAG 检索 + 注入系统提示词 + 推送 rag_retrieve 事件） | order=102 |
| `HistoryChatMemoryAdvisor` | **自定义**：全量聊天记录入库 + 工具调用审计日志 + tool_call 事件推送 | order=103，流式拦截 ToolCall 事件 |
| `SimpleLoggerAdvisor` | 请求/响应日志 | order=104 |

### 5.2 存储与记忆

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `SessionService` | Spring AI 2.0 原生会话服务（JDBC 仓储） | 取代原 `RedisCachedAndMysqlMemoryRepository` 双层存储 |
| `smallChatClient` | **概述小模型** ChatClient Bean | 记忆提取/决策、会话标题生成 |
| `AiAgentToolCallLog` / `IAiAgentToolCallLogService` | **新增**：工具调用审计日志实体与服务 | 落库 `ai_agent_tool_call_log`，与业务聊天记录解耦 |
| `MemoryType` / `MemoryOperation` / `ExtractedMemory` | **新增**：Mem0 记忆模型枚举与提取产物 | PROFILE/FACT + ADD/UPDATE/DELETE/NOOP |

### 5.3 RAG 检索与文档解析

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `RagContextQueryAdvisor` | **自定义**：知识库上下文查询 Advisor，在请求链路内执行三步检索并注入系统提示词 | 参考 VectorStoreChatMemoryAdvisor 模式，推送 rag_retrieve 事件 |
| `RagRetrieveContextService` | 三步检索：先查 tag 再执行（前置检查→反向匹配→内存模糊匹配→全量降级）→ knowledgeId 单重过滤向量检索 → Reranker 重排序 | 供外部 API（KbRagRetrieveApi）和 NL2SQL 工具（kbType=20）调用 |
| `RerankerService` | 调用外部 Reranker 模型重排序 | 降级机制 |
| `KbDocumentServiceImpl` | 文档上传流程编排（调用 ExtractorFactory + ChunkerFactory） | 责任精简：仅负责上传/删除/向量化流程，解析和分块已独立 |
| `ExtractorFactory` | 文档提取器工厂（按文件后缀自动路由） | Spring Bean 自动发现所有 Extractor |
| `AbstractTikaExtractor` | Tika 抽象基类（结构化 Markdown + 纯文本 + 输出上限降级） | PdfExtractor/WordExtractor/ExcelExtractor 共用 |
| `WordExtractor` | Word 文档提取（docx: POI 本地解析; doc: Tika 回退） | 958 行，标题/列表/表格/图片 OCR |
| `ExcelExtractor` | Excel/CSV 提取（POI全量 + FastExcel流式 + CSV状态机） | 编码嗅探、合并单元格、Tika兆底 |
| `PdfExtractor` | PDF 提取（Tika 结构化 Markdown） | 标题/表格/列表保留，输出上限防 OOM |
| `ImageExtractor` | 图片 OCR（本地 MinerU-OCR 视觉模型） | opendatalab/MinerU2.5-Pro-2605-1.2B |
| `AudioExtractor` | 音频元数据提取（Tika Metadata） | 支持 MP3/WAV/FLAC/OGG 等 |
| `ChunkerFactory` | 分块策略工厂（按 @Order 自动选择） | Spring Bean 自动发现所有 Chunker |
| `SemanticChunker` | 语义分块 @Order(10)（多尺度滑动窗口 + 动态阈值） | EmbeddingModel 不可用时自动降级 |
| `SeparatorChunker` | 分隔符分块 @Order(20)（正则/字面量） | 超长块用 TokenTextSplitter 兜底细切 |
| `TokenChunker` | Token 固定分块 @Order(30)（TokenTextSplitter） | 默认兜底分块策略 |

### 5.4 工具编排

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `WrappedMcpToolCallbackProvider` | MCP 工具包装器提供者 | 注入 dataCacheService |
| `ReturnDirectToolCallbackWrapper` | 工具调用拦截器 | 输入/输出双向拦截 |
| `AgentEventSinkManager` | SSE 旁路事件管理 | 工具调用状态推送 |

### 5.5 MCP 工具服务

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `BaseToolServiceImpl` | 工具抽象基类 | 模板方法模式 |
| `Nl2SqlToolServiceImpl` | 自然语言转 SQL | LLM 生成 + 自我修正 + 远程执行 |
| `DeptBizToolServiceImpl` | 部门业务聚合工具 | 多步操作封装为单一工具 |
| `ConfigToolServiceImpl` | 系统参数配置 CRUD + 查询 | 按 configKey/configName 查询，不暴露 ID |
| `DictToolServiceImpl` | 字典类型/字典数据 CRUD + 查询 | 支持 dictType/dictName/dictLabel 多维度查询 |
| `JobToolServiceImpl` | 定时任务 CRUD + 状态修改 + 立即执行 + 日志查询 | 支持 add/update/delete/changeStatus/run 5 种操作 |
| `LogToolServiceImpl` | 操作日志/登录日志查询 + 登录失败统计 | 只读查询，最多返回 10 条摘要 |
| `McpServerConfig` | MCP Server 配置 | 工具注册 + sqlChatClient Bean |

### 5.6 MCP 网关服务

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `MallAiMcpGatewayApplication` | 网关启动类 | 基于 spring-ai-alibaba-starter-mcp-gateway |
| `GatewayController` | 工具列表查询接口 | `GET /api/gateway/tools` 列出所有聚合工具 |
| `WebClientConfig` | WebClient 配置 | 异步 HTTP 客户端 |
| `WebClientStreamableHttpTransport` | MCP 传输层 | Streamable HTTP 传输实现 |
| `NacosMcpGatewayToolCallback` | **覆盖类**：修复 MCP 工具调用硬编码超时（20s/30s → 可配置 600s） | 同包同名覆盖依赖 jar，`mcp.gateway.tool-timeout-seconds` 控制 |

### 5.7 提示词管理（Nacos）

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `NacosPromptRegistry` | **新增**：Nacos Prompt 拉取与订阅服务 | `AiService.subscribePrompt()` 订阅 + 本地缓存 + Last-Known-Good 降级 |
| `PromptProperties` | **新增**：Nacos Prompt 绑定配置属性类 | `spring.ai.nacos.prompt.bindings`，支持 key/version/label/required |

### 5.8 NL2SQL 语义层

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `Nl2sqlMetricController` / `Nl2sqlMetricServiceImpl` | **新增**：指标定义 CRUD 管理 | 落库 `nl2sql_metric`，mcp-server Feign 直查 |
| `Nl2sqlDimensionController` / `Nl2sqlDimensionServiceImpl` | **新增**：维度定义 CRUD 管理 | 落库 `nl2sql_dimension`，含维度枚举值 |
| `Nl2sqlBusinessRuleController` / `Nl2sqlBusinessRuleServiceImpl` | **新增**：业务规则 CRUD 管理 | 落库 `nl2sql_business_rule` |
| `Nl2sqlEvalController` / `Nl2sqlEvalServiceImpl` | **新增**：评测用例 CRUD 管理 | 落库 `nl2sql_eval`，替代原裸 SQL 查询 |
| `RemoteNl2sqlMetricService` / `RemoteNl2sqlDimensionService` / `RemoteNl2sqlBusinessRuleService` / `RemoteNl2sqlEvalService` | **新增**：mcp-server 侧 Feign 客户端 | 指向 mall-ai-chat 内部 API |

---

## 6. 配置参考

### 6.1 mall-ai-chat (bootstrap.yml)

```yaml
spring:
  ai:
    nacos:
      prompt:                                # Nacos 提示词管理（详见 2.2）
        server-addr: 114.132.102.8:8848
        namespace-id: public
        username: nacos
        password: nacos
        transport-mode: http
        bindings:
          system-prompt:
            key: system-prompt
            label: latest
          VectorStoreChatMemoryPrompt:
            key: VectorStoreChatMemoryPrompt
            label: latest
          TitleCompressPrompt:
            key: TitleCompressPrompt
            label: latest
          DecideInstructionPrompt:
            key: DecideInstructionPrompt
            label: latest
          ExtractInstructionPrompt:
            key: ExtractInstructionPrompt
            label: latest
    model:
      chat: openai
      embedding: openai
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        model: kimi-k2.7-code
      embedding:
        model: Qwen3-Embedding-4B-Q8_0
        base-url: http://127.0.0.1:8889/v1
    mcp:
      client:
        enabled: true
        type: ASYNC
        request-timeout: 600s
        streamable-http:
          connections:
            gateway:
              url: http://localhost:9999      # 通过网关访问 MCP Server
            mcp-echarts:
              url: http://114.132.102.8:2001  # 自部署 ECharts MCP
              endpoint: /mcp
    session:                                 # 会话记忆（SessionMemoryAdvisor + SessionService）
      repository:
        jdbc:
          initialize-schema: always          # MySQL/PostgreSQL 必须显式开启建表
      time-to-live: 30d                      # 会话有效期，默认 60d，支持 ISO-8601
    vectorstore:
      weaviate:
        host: 114.132.102.8:18080
        scheme: http
        api-key: b251055070805a857b31dd014d40b727dd6a23714ea1bf66

vectorstore:
  enabled: true                              # 开启全局向量功能
  chat-memory-default-topk: 1               # 向量记忆检索条数
  weaviate:
    knowledge-object-class: KnowledgeBase
    chat-memory-object-class: ConversationHistory
    tool-index-object-class: ToolIndex

smallmodel:                                  # 概述小模型（记忆提取/合并 + 会话标题生成）
  base-url: http://114.132.102.8:8888/v1
  api-key: 123456
  model: MiniCPM5-1B-Q8_0

reranker:
  enabled: false                             # 重排序开关
  base-url: http://127.0.0.1:8887
  top-n: 3

mineru:
  base-url: https://mineru.net
  token: xxx
  model-version: vlm
  poll-interval-ms: 2000
  poll-max-attempts: 120
  vl:
    base-url: http://127.0.0.1:8890/v1       # 本地 MinerU-OCR 视觉模型（图片 OCR）
    api-key: 123456
    model: opendatalab/MinerU2.5-Pro-2605-1.2B

extract:
  tika-max-chars: 2097152                    # Tika 解析输出上限（2M 字符 ≈ 4MB 内存）
  images: true                               # Word 图片提取总开关（批量灌库时可关闭提速）
  page-break-mark: false                     # Word 分页标记开关

ai:
  chat:
    stream-timeout-seconds: 600              # SSE 流式超时
  tool:
    cache:
      threshold: 2000                        # 工具数据缓存阈值
      ttl-hours: 1                           # 缓存过期时间
```

### 6.2 mall-ai-mcp-server (bootstrap.yml)

> **配置迁移说明**：`spring.ai.*`（模型、MCP Server、Nacos 注册）配置已从本地 bootstrap.yml **整体注释**，改由 **Nacos 配置中心**（`mall-ai-mcp-server-dev.yml`）统一管理，本地仅保留 Nacos 连接与日志配置。以下为参考配置（需在 Nacos 中维护）：

```yaml
spring:
  ai:
    model:
      chat: openai
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        model: qwen3.7-flash
    mcp:
      server:
        name: mall-ai-mcp-server
        protocol: STREAMABLE
        request-timeout: 60s            # 需大于 nl2sql.time-budget-ms 总时延预算
        streamable-http:
          mcp-endpoint: /mcp
          keep-alive-interval: 30s
    alibaba:
      mcp:
        nacos:
          server-addr: 114.132.102.8:8848
          namespace: public
          username: nacos
          password: nacos
          register:
            enabled: true
            service-name: ${spring.application.name}
```

### 6.3 mall-ai-mcp-gateway (bootstrap.yml)

> **配置迁移说明**：`spring.ai.*`（MCP Server、Alibaba Gateway、Nacos 注册）配置已从本地 bootstrap.yml **整体注释**，改由 **Nacos 配置中心**（`mall-ai-mcp-gateway-dev.yml`）统一管理。以下为参考配置（需在 Nacos 中维护）：

```yaml
server:
  port: 9999

spring:
  application:
    name: mall-ai-mcp-gateway
  cloud:
    nacos:
      discovery:
        server-addr: 114.132.102.8:8848  # 注册到 Nacos
      config:
        server-addr: 114.132.102.8:8848
  ai:
    mcp:
      server:
        name: ${spring.application.name}
        protocol: STREAMABLE
        type: async
        streamable-http:
          mcp-endpoint: /mcp
          keep-alive-interval: 60s
    alibaba:
      mcp:
        gateway:
          enabled: true                   # 启用 Spring AI Alibaba MCP Gateway
          nacos:
            serviceNames:                 # 聚合的 MCP Server 服务名列表
              - mall-ai-mcp-server
          streamable:
            enabled: true

mcp:
  gateway:
    tool-timeout-seconds: 600             # 工具调用超时（秒），覆盖类 NacosMcpGatewayToolCallback 读取
```

---

## 7. 注意事项与最佳实践

### 7.1 向量库成本与清理（高危）

*   **存储成本**：向量库不仅存文本，还存高维浮点数组，磁盘占用是 MySQL 的 10 倍以上。
*   **必须清理**：切勿永久存储所有向量。长期记忆默认 30 天 TTL（`expireAt` 字段），检索时过滤过期记忆。
*   **数据一致性**：向量库更新是异步的，消息入库后立刻查询可能查不到（延迟问题）。
*   **记忆决策机制**：`VectorStoreChatMemoryAdvisor` 采用 Mem0 两阶段模型（提取 → 决策），写入时按相似度检索候选并由小模型判定 `ADD/UPDATE/DELETE/NOOP`（profile 覆盖、fact 追加、完全相同续期），替代原批量压缩方案。

### 7.2 隐私隔离（安全）

*   **向量检索隔离**：在调用 VectorStore 搜索时，必须在 Filter 中加入 `userId`（长期记忆）或 `knowledgeId`（知识库）。否则用户 A 可能搜到用户 B 的隐私记录。
*   **推荐做法**：在写入 Document 时 metadata 强制加入 userId / knowledgeId，查询时强制过滤。
*   **长期记忆作用域**：`VectorStoreChatMemoryAdvisor` 检索表达式为 `userId = {userId} AND status = active AND expireAt > now`，缺失 userId 时兜底为 `anonymous`（生产环境应在入口处保证 userId 必传）。

### 7.3 会话记忆与压缩

*   近期上下文由 `SessionMemoryAdvisor` + `SessionService`（JDBC）承载，`TurnCountTrigger(4)` 每 4 轮触发压缩，`SlidingWindowCompactionStrategy(maxEvents=8)` 保留最近 8 个事件。
*   **开发建议**：调整压缩粒度时修改 `ChatClientConfig.sessionMemoryAdvisor()` 的 `TurnCountTrigger` / `maxEvents` 参数；会话有效期由 `spring.ai.session.time-to-live`（默认 30d）控制。

### 7.4 事务一致性

*   MySQL 全量表（`sys_chat_history`）、会话表（`SessionService` JDBC）与工具审计表（`ai_agent_tool_call_log`）不在同一个事务中。如果全量入库成功但会话写入失败，用户可能看到历史记录但 AI "失忆"。
*   **优化**：优先保证会话写入成功，全量入库/审计入库失败可记录日志异步重试。

### 7.5 工具调用安全

*   **NL2SQL 安全校验**：JSqlParser AST 校验（只允许 SELECT）+ 表名白名单校验（防止 LLM 幻觉出不存在的表）+ 自动 LIMIT 100。
*   **按名称查询**：所有工具不接受 ID 直传，防止模型猜测 ID 导致误操作。
*   **dataId 过期**：工具缓存数据 TTL 1小时，防止 Redis 内存泄漏。

### 7.6 SSE 流式注意事项

*   **超时控制**：设置 600 秒超时，防止大模型卡死导致连接挂死。
*   **客户端断开**：通过 `doOnCancel` 处理客户端主动断开。
*   **异常脱敏**：`onErrorResume` 捕获异常后返回通用错误信息，不暴露内部细节。
*   **事件头**：`tool_call` / `rag_retrieve` 事件显式设置 SSE `event` 头，前端按事件类型路由后解析 JSON 载荷。

### 7.7 MCP 工具调用超时（多层超时链）

MCP 工具调用存在**多层超时**，需逐层配置避免"上层先超时"：

| 层级 | 配置项 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| mall-ai-chat MCP Client | `spring.ai.mcp.client.request-timeout` | 600s | chat 侧请求超时 |
| mall-ai-mcp-gateway 工具回调 | `mcp.gateway.tool-timeout-seconds` | 600s | **覆盖类** `NacosMcpGatewayToolCallback` 读取（原硬编码 20s/30s） |
| mall-ai-mcp-server MCP Server | `spring.ai.mcp.server.request-timeout` | 60s | 需大于 `nl2sql.time-budget-ms`（90s 时需同步调大） |
| NL2SQL 总时延预算 | `nl2sql.time-budget-ms` | 90000ms | 召回+生成+重试全流程共享 |

> **经验法则**：`chat 侧超时 > gateway 工具超时 > mcp-server 超时 > nl2sql 时延预算`，否则内层先超时会导致外层收到错误而非结果。

### 7.8 Nacos 提示词管理注意事项

*   **启动依赖**：`required=true` 的 Prompt 绑定启动加载失败会直接报错，确保 Nacos 中已发布对应 Prompt（key + label/version）。
*   **热更新**：Nacos 控制台发布新版本后订阅回调自动刷新本地缓存，无需重启；MD5 去重避免重复替换。
*   **渲染告警**：渲染后残留 `{{xxx}}` 占位符会输出告警日志，用于排查漏传变量。
*   **配置迁移**：mall-ai-mcp-server / mall-ai-mcp-gateway 的 `spring.ai.*` 配置已迁移至 Nacos 配置中心（`{服务名}-dev.yml`），修改配置需在 Nacos 控制台操作并发布。
