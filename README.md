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
| 文档解析（PDF/Word） | MinerU 远程接口（mineru.net，vlm 模式） |
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
│  │ (SSE流式) │  │ (Builder) │  │ (6层拦截) │  │(Qwen3.7)│ │
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
│  │Redis+MySQL│                   │  MCP Client (Async)  │
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
├── config/
│   ├── ChatClientConfig.java           # ChatClient Bean 配置（核心，含 smallChatClient 概述小模型）
│   ├── VectorStoreConfig.java          # 三个 VectorStore Bean（字段常量收敛到 ChatConstants）
│   ├── AgentEventSinkManager.java      # SSE 旁路推送管理（tool_call / rag_retrieve 事件）
│   └── SaLlmConfig.java                # 会话记忆配置（ChatMemory + MinerU RestClient + 线程池）
├── constant/
│   └── ChatConstants.java              # 向量库字段常量 + Advisor context key 统一契约
├── advisor/
│   ├── VectorStoreChatMemoryAdvisor.java       # 长期语义记忆 Advisor（userId 跨会话 + 异步写入 + upsert 合并）
│   ├── RagContextQueryAdvisor.java             # 知识库上下文查询 Advisor（RAG 检索 + 注入系统提示词 + rag_retrieve 事件）
│   ├── FullHistoryChatMemoryAdvisor.java       # 全量历史记录 Advisor
│   ├── ReturnDirectChatMemoryAdvisor.java      # returnDirect 工具结果 Advisor
│   ├── RedisCachedAndMysqlMemoryRepository.java # Redis+MySQL 双层存储
│   ├── WrappedMcpToolCallbackProvider.java     # MCP 工具包装器
│   └── ReturnDirectToolCallbackWrapper.java    # 工具调用拦截器（dataId 缓存）
├── controller/
│   ├── ChatAgent.java                  # 聊天入口（SSE 流式，支持 userId 参数）
│   ├── AiConversationController.java   # 会话管理（创建/删除/列表）
│   ├── SpringAiChatMemoryController.java # 窗口记忆管理（JDBC表CRUD）
│   ├── KbDocumentController.java       # 知识库文档管理
│   ├── KbKnowledgeBaseController.java  # 知识库管理
│   └── SysChatHistoryController.java   # 聊天历史
├── service/impl/
│   ├── AiConversationServiceImpl.java  # 会话管理（创建+异步标题生成+级联删除）
│   ├── ChatAgentService.java           # LLM 流式阶段（RAG 已下沉到 Advisor）
│   ├── RagRetrieveContextService.java  # RAG 检索（供外部 API / NL2SQL 工具调用）
│   ├── RerankerService.java            # Reranker 重排序服务
│   ├── ToolDataCacheService.java       # 工具大数据 Redis 缓存
│   ├── KbDocumentServiceImpl.java      # 文档解析+切片+向量化
│   ├── MinerUService.java              # MinerU 文档解析
│   └── ...
└── domain/
    ├── ChatStreamEvent.java            # SSE 事件结构
    ├── ChatRequest.java                # 聊天请求（question + conversationId + userId）
    ├── SysChatHistory.java             # 聊天历史实体（全量记录）
    ├── AiConversation.java             # 会话实体（userId+conversationId+title）
    ├── SpringAiChatMemory.java         # 窗口记忆实体（JDBC表）
    ├── KbDocument.java                 # 知识库文档实体
    └── ...
```

### 2.2 系统全局提示词

*   **实现方式**：在构建 `ChatClient` Bean 时，通过 `defaultSystem` 统一加载 Markdown 文件。
*   **文件位置**：`resources/prompts/system-prompt-simplify.md`
*   **加载时机**：应用启动时加载一次，避免每次请求重复 IO。
*   **配置要点**：将 System Prompt 定义为独立的 Markdown 文件，便于版本控制和热更新。

**当前 System Prompt 内容**：
```markdown
# Role
假维斯，一个未通过正版验证的盗版贾维斯。后台管理助手，执行力强。
# Tools & Skills
你拥有以下系统工具的调用权限：菜单导航、数据查询、数据增删改
# Instructions
1. 立即执行：识别意图后直接调用工具，不要反问用户（除非缺少关键参数）。
2. 简洁回复：对话保持简短直接。
# Echarts Protocol
调用图表工具时，参数中必须且只能显式包含 "outputType": "option"。
# Constraints
- 严禁编造工具或参数。
- 工具调用失败则立即停止并告知用户
```

### 2.3 Advisor 链（核心拦截层）

Advisor 链是 Agent 编排的核心，7 个 Advisor 按 order 排序依次执行：

| 顺序 | Advisor | Order | 职责 |
| :--- | :--- | :--- | :--- |
| 1 | `MessageChatMemoryAdvisor` | HIGHEST+200 | 近期上下文读写（Redis+MySQL 双层） |
| 2 | `VectorStoreChatMemoryAdvisor` | HIGHEST+201 | 长期语义记忆检索（Weaviate） |
| 3 | `RagContextQueryAdvisor` | HIGHEST+201 | 知识库上下文查询（RAG 检索 + 注入系统提示词 + 推送 rag_retrieve 事件） |
| 4 | `ReturnDirectChatMemoryAdvisor` | HIGHEST+202 | 拦截 `returnDirect=true` 的工具结果，单独入库 |
| 5 | `ToolSearchToolCallingAdvisor` | HIGHEST+300 | 工具动态检索（每次仅注入相关工具，详见 2.8.2） |
| 6 | `FullHistoryChatMemoryAdvisor` | 1 | 全量聊天记录入库 MySQL + 工具调用事件推送 |
| 7 | `SimpleLoggerAdvisor` | 2 | 请求/响应日志 |

#### 2.3.1 FullHistoryChatMemoryAdvisor

**核心职责**：将每条消息（User/Assistant/Tool）全量存入 MySQL `sys_chat_history` 表。

**关键逻辑**：
- **before 阶段**：从 ChatMemory 加载历史消息，注入到 Prompt 中；同时将用户消息存入 MySQL
- **流式拦截**：检测 LLM 发起的工具调用，通过 `AgentEventSinkManager` 推送 `tool_call` 事件给前端
- **after 阶段**：流式聚合完成后，将 Assistant 回复存入 MySQL
- **序列号机制**：通过 Redis `INCR` 生成全局递增 `sequenceId`，保证消息顺序
- **工具调用存储**：`AssistantMessage` 中的 `ToolCalls` 序列化为 JSON 存入 `tool_calls` 字段；`ToolResponseMessage` 的响应数据同样存入

#### 2.3.2 ReturnDirectChatMemoryAdvisor

**核心职责**：当工具标记了 `returnDirect=true`（description 以 `[JSON]` 结尾）时，工具结果不经过 LLM 处理，直接返回给前端。此 Advisor 拦截这类结果并单独入库。

**判断逻辑**：检查 `ChatGenerationMetadata.finishReason == "returnDirect"`。

#### 2.3.3 VectorStoreChatMemoryAdvisor（长期语义记忆）

**核心职责**：将用户消息异步提取为"记忆事实"写入 Weaviate，并在进入模型前按 **userId** 检索生效中的长期记忆注入系统提示词。**作用域从 conversationId 改为 userId，长期记忆跨会话生效**。

**关键设计**：

| 设计点 | 说明 |
| :--- | :--- |
| 作用域 | `userId`（跨会话检索主过滤字段），`conversationId` 仅随 metadata 落库用于追踪 |
| 异步写入 | `storeMemoryAsync()` 运行在 advisor 自身 scheduler 上，不阻塞请求链路，首 token 延迟不受摘要/查重/写入影响 |
| 记忆提取 | `summarizeMessage()` 调用 `smallChatClient`（概述小模型）将用户消息总结为一句以"用户"开头的短句；无实质信息（输出"无"）跳过写入 |
| 记忆合并 | `upsertMemoryItem()` 采用 Dify op 模型：无相近 → ADD；精确相同 → NOOP（纯代码续期 TTL）；语义相近 → `mergeMemory()` 单对判定合并（相同/冲突存新替换旧，不同则 ADD） |
| 记忆有效期 | 默认 30 天（`DEFAULT_MEMORY_TTL_MS`），写入时推导 `expireAt`；检索时过滤 `expireAt > now`，过期记忆不注入模型 |
| 降级 | 向量库不可用时降级为无记忆继续对话，不中断请求 |
| 摘要输入上限 | 超长消息（>200 字符）先截断，避免塞爆摘要 prompt |
| AI 回复不入库 | 只存用户消息；AI 回复是通用知识/任务结果，写入会污染记忆库 |

**记忆 Schema 字段**（与 `VectorStoreConfig` 会话记忆库对齐，常量收敛到 `ChatConstants`）：

| 字段 | 说明 |
| :--- | :--- |
| `userId` | 记忆归属用户 ID，长期记忆跨会话检索的基础过滤字段 |
| `conversationId` | 归属会话 ID，仅随 metadata 落库用于追踪 |
| `messageType` | 消息角色（USER / ASSISTANT / SYSTEM） |
| `status` | 记忆状态（active / archived / superseded / expired，用字符串而非 boolean） |
| `ingestedAt` | 记忆写入时间戳（毫秒） |
| `expireAt` | 记忆过期时间戳（毫秒），0 表示不过期 |

**检索过滤表达式**：`userId = {userId} AND status = active AND expireAt > now`

#### 2.3.4 RedisCachedAndMysqlMemoryRepository

**核心职责**：实现 `ChatMemoryRepository` 接口，提供 Redis + MySQL 双层存储。

**读写策略**：
- **读取**：先查 Redis（TTL 7天），未命中查 MySQL 并回填 Redis
- **写入**：先落 MySQL，再更新 Redis
- **降级**：Redis 异常时直接查 MySQL，不影响主流程
- **序列化**：使用 fastjson2 `WriteClassName` 特性，在 JSON 中写入 `@type` 字段保留消息类型信息

### 2.4 会话管理与级联操作

#### 2.4.1 会话创建与异步标题生成

`AiConversationServiceImpl.createAiConversation()` 流程：

1. 生成 UUID 作为 conversationId，建立 userId 与 conversationId 的关联
2. **直接将用户第一条消息 `question` 作为会话标题**写入 `ai_conversation.title`
3. 写入 Redis（`chat:conversation:{conversationId}` → userId，TTL 7天）
4. 前端在已有有效标题（非空、非"未命名对话"）时不再覆盖标题，仅对空标题或占位符生成新标题

**标题生成（smallChatClient）**：
- 使用 `smallChatClient`（概述小模型，替代原 `titleChatClient`）异步生成标题
- 内置标题生成 System Prompt：不超过 15 字、概括主题、不要标点结尾、只输出标题本身
- **后处理**：取首行、去空白、截断到 30 字，防止模型输出多余内容污染标题

**关键类**：`AiConversationServiceImpl`、前端 `index.vue`（标题覆盖保护逻辑）

#### 2.4.2 会话级联删除

`deleteByConversationId()` 执行 4 层级联清理：

| 层级 | 操作 | 存储位置 |
| :--- | :--- | :--- |
| 1. 关联表 | 删除 userId ↔ conversationId 关联 | MySQL `ai_conversation` |
| 2. 窗口记忆 | 删除滑动窗口数据 | MySQL `spring_ai_chat_memory` + Redis |
| 3. 全量历史 | 删除所有聊天记录 | MySQL `sys_chat_history` + Redis 序列号 |
| 4. 向量数据 | 删除会话向量 + 工具索引向量 | Weaviate（conversationId + sessionId 过滤） |

**Redis Key 清理**：
- `chat:conversation:{conversationId}` — 会话关联
- `chat:memory:{conversationId}` — 窗口记忆缓存
- `seq:chat:memory:{conversationId}` — 消息序列号

#### 2.4.3 SaLlmConfig 配置

`SaLlmConfig` 定义了三个核心 Bean：

| Bean | 类型 | 说明 |
| :--- | :--- | :--- |
| `chatMemory` | `MessageWindowChatMemory` | 窗口记忆，基于 `RedisCachedAndMysqlMemoryRepository` |
| `mineruRestClient` | `RestClient` | MinerU API 客户端（连接超时10s，读取超时60s） |
| `taskExecutor` | `Executor` | 异步线程池（核心5/最大10/队列100），用于标题生成和向量压缩 |

### 2.5 会话记忆与上下文管理

#### 2.5.1 多用户会话隔离

*   **隔离维度**：`userId`（用户维度的数据权限） + `conversationId`（单次会话维度的上下文连续性）。
*   **实现机制**：
    *   当用户第一次输入内容进行请求，创建 conversationId 并与用户 id 建立关联关系
    *   通过 `ChatMemory.CONVERSATION_ID` 上下文传递会话 ID
    *   在所有存储层（Redis, MySQL, VectorStore）的数据写入时，必须带上 CONVERSATION_ID

#### 2.5.2 上下文窗口（近期记忆 - Window Memory）

这是 AI "正在看"的内容，决定了对话的连续性。

*   **窗口策略**：基于 `maxMessages` 滑动窗口（配置：`chat-memory.max-messages: 4`）。
    *   **逻辑**：仅保留最近的 N 条消息。达到上限时，自动移除最早的一轮对话。
    *   **奇偶校验**：为了保证"一问一答"的完整性，底层会自动将奇数 `maxMessages` 向下取整为偶数。
*   **存储实现（双写策略）**：
    1.  **Redis 缓存层**：提供毫秒级读写，Key 设置 TTL 7天，过期自动清理。
    2.  **MySQL 持久化层**：作为 Redis 的持久化备份，Redis 数据过期后可通过查询回填缓存。

#### 2.5.3 向量库全量上下文（长期记忆 - Vector Memory）

这是 AI "能想起"的内容，实现了跨会话、长期的语义检索。

*   **应用场景**：用户询问"我最初问了什么"，即使该对话不在当前窗口，AI 也能通过向量检索找到。
*   **技术栈**：本地 Embedding 模型（Qwen3-Embedding-4B） + Weaviate 向量数据库。
*   **检索逻辑**：基于 `VectorStoreChatMemoryAdvisor`，按 **userId** 检索生效中的长期记忆（`status=active AND expireAt > now`）。
    *   **参数**：`defaultTopK`（配置：`vectorstore.chat-memory-default-topk: 1`）。
*   **记忆写入（异步）**：用户消息经 `smallChatClient` 提取为"记忆事实"短句后异步写入向量库，不阻塞请求链路。
*   **记忆合并（upsert）**：写入时按相似度查重——无相近 → 新增；精确相同 → 纯代码续期 TTL；语义相近 → 调用小模型合并（冲突以新记忆为准，不同话题合并为一句）。
*   **记忆有效期**：默认 30 天（`expireAt` 字段），过期记忆不注入模型。
*   **AI 回复不入库**：只存用户消息，避免通用知识/任务结果污染记忆库。
*   **架构演进**：原 `VectorCompressionService`（批量压缩）已移除，由上述"单条 upsert 合并"机制取代。

#### 2.5.4 全量聊天记录（业务展示）

*   **作用**：供前端展示"历史会话列表"和"聊天详情"，支持分页、关键词搜索。
*   **存储**：MySQL 业务表 `sys_chat_history`，包含 `conversation_id`、`content`、`tool_calls`、`sequence_id`、`is_compression`、`type` 等字段。
*   **特点**：**全量永久存储**（除非用户主动删除），不进行滑动窗口截断。
*   **重点类**：`FullHistoryChatMemoryAdvisor`。

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
  ├─ Markdown (.md) → 直接读取内容
  ├─ 图片 (png/jpg/jpeg/gif) → 本地 MinerU-OCR 视觉模型解析（minerUChatClient）
  │                            └─ opendatalab/MinerU2.5-Pro-2605-1.2B @ 127.0.0.1:8890
  └─ PDF/Word → MinerU 远程接口解析为 Markdown（mineru.net，vlm 模式）
  │              └─ 内嵌图片 → 本地 MinerU-OCR 模型生成图片描述，替换回原文
  │
  ├─ 内容清洗 (移除 OCR 噪声、页码、乱码)
  ├─ 自定义分段符切分 (chunkSeparator，默认 \n\n)
  ├─ TokenTextSplitter 细切分 (chunkSize=500)
  ├─ 存入 Weaviate 向量库
  └─ 同步 MySQL chunk 记录
```

**两种解析路径说明**：

| 文件类型 | 解析方式 | 配置 | 关键类/Bean |
| :--- | :--- | :--- | :--- |
| `.md` | 直接 `Files.readString` | — | `KbDocumentServiceImpl` |
| 图片 (png/jpg/jpeg/gif) | 本地 MinerU-OCR 视觉模型 | `mineru.vl.base-url` (127.0.0.1:8890) | `minerUChatClient` |
| PDF/Word | MinerU 远程接口（vlm 模式） | `mineru.base-url` (mineru.net) | `MinerUService` |
| PDF/Word 内嵌图片 | 本地 MinerU-OCR 模型 | 同上 | `parseImageWithLLM()` |

**关键配置**：
- `mineru.base-url`：远程 MinerU 接口地址（用于 PDF/Word 解析）
- `mineru.vl.base-url`：本地 MinerU-OCR 模型地址（用于图片 OCR）
- `mineru.vl.model`：本地视觉模型名称（opendatalab/MinerU2.5-Pro-2605-1.2B）
- `mineru.model-version`：远程接口解析模式（vlm 推荐 / pipeline / MinerU-HTML）
- `chunkSeparator`：自定义分段符，前端可传入（如 `---`、`###`）
- `chunkSize`：分块大小，默认 500 token

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

##### 2.6.2.1 文档解析路由（extractText）

入口方法 `extractText(fileResource, filename)` 按文件后缀名（转小写）路由到不同提取器，参照 Dify 设计：

| 分支 | 判断条件 | 处理逻辑 |
| :--- | :--- | :--- |
| Markdown | `.md` 后缀 | `Files.readString` 直接读取文件内容为字符串 |
| 图片 | `isImageFile()`（png/jpg/jpeg/gif） | 调用 `parseImageWithLLM()` → 本地 MinerU-OCR |
| 复杂文档 | 其他（PDF/Word 等） | 调用 `minerUService.parseMarkdown()` → 远程 MinerU |

**图片解析 `parseImageWithLLM(resource)`**：
1. `guessMimeType(filename)` 推断 MIME 类型（pdf/jpg/png/gif，默认 png）
2. 构建 `Media` 对象（mimeType + data）
3. 调用 `minerUChatClient`，user prompt 固定为 `"1"`，`media` 为图片二进制
4. 返回 OCR 文本；异常时返回 `[图片解析失败]`，不中断主流程

**PDF/Word 内嵌图片处理**：
远程 MinerU 解析返回 `MinerUResult(markdown, images)`，对 `images` 列表逐个处理：
1. 调用 `parseImageWithLLM(new ByteArrayResource(imageData.data()))` 生成图片描述
2. 将 Markdown 中的占位符 `![](images/{name})` 替换为 `\n[图片说明: {描述}]\n`
3. 使用 `String.replace`（非正则），避免特殊字符转义问题

##### 2.6.2.2 内容清洗策略（cleanContent）

提取文本后执行 5 步规则清洗，移除 OCR 噪声：

| 步骤 | 正则 | 作用 |
| :--- | :--- | :--- |
| 1 | `(<\|txt_contd_tgt\|>\|<\|txt_contd_src\|>)+` | 移除 MinerU 特殊续文本标签 |
| 2 | `(?m)^\s*\d+\s*$` | 移除仅含数字的独立行（通常是页码） |
| 3 | `[■□▲△○●⊛※]{2,}` | 移除连续 2+ 个 OCR 噪声字符 |
| 4 | `([\u4e00-\u9fa5])\s+([\u4e00-\u9fa5])` → `$1$2` | 去除中文字符之间的多余空格 |
| 5 | `\n{3,}` → `\n\n` | 规范化空行，最多保留一个空行 |

最后执行 `trim()` 去除首尾空白。清洗后内容为空则抛出异常终止处理。

##### 2.6.2.3 分块策略（generalSmartSplit）

采用 **"段落粗切分 + Token 细切分"** 两阶段策略，兼顾语义完整与长度控制：

**阶段 1：段落粗切分（保证语义完整）**

- 分段符：优先使用 `kbDocument.getChunkSeparator()`，为空则默认 `\\n\\n`（空行）
- `text.split(separator)` 切分后，逐段 `trim()` 并过滤空段
- **降级逻辑**：若切分后仅剩 ≤1 段（说明文档无标准段落分隔），直接将原文档作为单段交给阶段 2

**阶段 2：Token 细切分（防止超长）**

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

**设计要点**：
- 先按段落切分保证语义边界完整，再按 Token 切分防止单块超长
- `punctuationMarks` 同时包含中英文标点，适配中文文档
- `minChunkSizeChars` 设为 `chunkSize` 的 10%，尽量保留上下文（参考 Dify overlap 设计）

##### 2.6.2.4 语义分块（Semantic Chunking）

在固定 Token/段落分块之外，系统新增了**语义分块**能力。通过 `kb_document` 表的 `semantic_chunking` 字段控制（`true`-语义分块，`false`-固定 token/段落分块），文档上传时前端可针对不同文档选择合适的分块策略。

**核心思想**：基于 Embedding 向量计算段落间的语义相似度，将**语义相近的段落合并为同一块**，避免固定长度切分导致语义断裂。例如：一段介绍"系统架构"的文字被固定切分到两个 chunk 中，检索时可能只命中一半，语义分块则能保证整段内容完整保留。

**算法流程**（`KbDocumentServiceImpl.semanticSplit()`）：

```
文档内容
  │
  ├─ 1. 切分为语义最小单元（splitIntoSemanticUnits）
  │   ├─ 优先按段落（空行/自定义分段符）切分
  │   └─ 段落超过 500 字符时，按句子进一步切分（splitBySentences）
  │       └─ 支持中英文标点（。！？!?；;），短句自动合并（<100字符）
  │
  ├─ 2. 计算每个单元的 Embedding 向量
  │   ├─ 调用本地 Embedding 模型（Qwen3-Embedding-4B）
  │   └─ 分批调用（batchSize=20），避免单次请求过大
  │
  ├─ 3. 计算相邻单元的余弦相似度（cosineSimilarity）
  │   └─ 相似度 < 阈值（0.75）→ 该处为语义边界，标记切分点
  │
  ├─ 4. 按切分点合并单元，形成语义块
  │   └─ 相邻语义相近的段落合并为同一 chunk
  │
  └─ 5. 对过大的语义块按 Token 细切分
      └─ 超过 chunkSize × 2 的块，用 TokenTextSplitter 再切分
```

**关键参数**：

| 参数 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `semanticChunking` | `false` | 是否启用语义分块（`kb_document` 表字段） |
| `threshold` | `0.75` | 语义相似度阈值，低于该值则切分（0~1，值越小越容易切分） |
| `chunkSize` | `500` | 语义块最大 Token 数，超过 `chunkSize × 2` 再细切分 |
| `batchSize` | `20` | Embedding 分批调用大小，避免单次请求过大 |
| 段落超长阈值 | `500` 字符 | 超过则按句子进一步切分 |
| 短句合并阈值 | `100` 字符 | 相邻短句自动合并，避免产生过小单元 |

**降级机制**：

| 场景 | 降级策略 |
| :--- | :--- |
| 未配置 `EmbeddingModel` | 降级为 Token 分块（`tokenSplitFallback()`） |
| Embedding 计算异常 | 捕获异常，降级为 Token 分块 |
| 文档仅 1 个语义单元 | 直接返回该单元，不执行向量计算 |

**与固定分块的对比**：

| 维度 | 固定 Token/段落分块 | 语义分块 |
| :--- | :--- | :--- |
| 切分依据 | 段落符 + Token 数量 | Embedding 向量语义相似度 |
| 语义完整性 | 可能切断语义边界 | 语义相近段落自动合并 |
| 计算开销 | 低（纯文本处理） | 高（需调用 Embedding 模型） |
| 适用场景 | 结构规整、段落分明的文档 | 语义连贯、段落边界模糊的文档 |
| 检索精度 | 依赖固定切分质量 | 语义块更完整，检索命中率更高 |

**关键实现方法**（`KbDocumentServiceImpl`）：

| 方法 | 职责 |
| :--- | :--- |
| `generalSmartSplit()` | 分块入口，根据 `semanticChunking` 选择语义分块或固定分块 |
| `semanticSplit()` | 语义分块主流程：切单元 → 向量化 → 相似度计算 → 合并 → 细切分 |
| `splitIntoSemanticUnits()` | 将文本切分为语义最小单元（段落优先，超长按句子） |
| `splitBySentences()` | 按中英文标点切分句子，短句自动合并 |
| `cosineSimilarity()` | 计算两个 Embedding 向量的余弦相似度 |
| `tokenSplitFallback()` | 降级方案：Embedding 不可用时回退到 Token 分块 |

### 2.7 SSE 流式推送

#### 2.7.1 双通道架构

```
ChatAgent.chatStream()
  │
  ├─ 主流：LLM 文本流 → chunk 事件 → 前端
  │
  └─ 旁路：FullHistoryChatMemoryAdvisor 拦截工具调用
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

**解决方案**：使用 Spring AI 2.0 的 `ToolSearchToolCallingAdvisor`，采用 **"渐进式工具披露"（Progressive Tool Disclosure）** 模式——初始只给 LLM 一个内置的 `toolSearchTool`，由 LLM 按需主动搜索并发现业务工具。

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
| 1 | MessageChatMemoryAdvisor | HIGHEST+200 | 近期上下文读写 |
| 2 | VectorStoreChatMemoryAdvisor | HIGHEST+201 | 长期语义记忆 |
| 3 | ReturnDirectChatMemoryAdvisor | HIGHEST+202 | returnDirect 工具结果拦截 |
| **4** | **ToolSearchToolCallingAdvisor** | **HIGHEST+300** | **工具动态检索注入** |
| 5 | FullHistoryChatMemoryAdvisor | 1 | 全量记录 + 工具调用事件推送 |
| 6 | SimpleLoggerAdvisor | 2 | 请求/响应日志 |

**顺序设计要点**：
- ToolSearch 的默认 order 是 `HIGHEST_PRECEDENCE + 300`（Spring AI 2.0 内置值）
- 位于记忆 Advisor 之后：确保记忆已加载完成再搜索工具
- 位于历史记录 Advisor 之前：确保 FullHistoryChatMemoryAdvisor 能拿到最终的工具调用信息
- ToolSearch 之后的 Advisor（order=1, 2）位于循环**内部**，每轮迭代都会执行

##### 关键类

| 类/Bean | 职责 |
| :--- | :--- |
| `ToolSearchToolCallingAdvisor` | 递归 Advisor，索引工具 + 注入 toolSearchTool + 拦截搜索调用 |
| `ToolIndex` | 工具索引接口（`search(query, maxResults)`） |
| `LuceneToolIndex` | Lucene 实现，本地倒排索引，阈值 0.3 |
| `VectorToolIndex` | 向量库实现，基于 Weaviate 语义检索 |
| `RegexToolIndex` | 正则实现，按工具名模式匹配（本项目未使用） |
| `ChatClientConfig` | 配置类，注册 ToolIndex 和 ToolSearchAdvisor Bean |

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
│   └── SysNoticeBo.java                # 公告工具参数
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
        ├── UserToolServiceImpl.java    # 用户 CRUD + 状态管理
        ├── RoleToolServiceImpl.java    # 角色 CRUD
        ├── NoticePostToolServiceImpl.java # 公告+岗位 CRUD
        ├── OpenMenuToolServiceImpl.java   # 菜单导航
        ├── DeptBizToolServiceImpl.java    # 部门业务聚合工具
        └── Nl2SqlToolServiceImpl.java     # 自然语言转 SQL
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
| `RoleToolServiceImpl` | `roleCrud` | 角色增删改查 |
| `NoticePostToolServiceImpl` | `noticeCrud` / `postCrud` | 公告/岗位增删改查 |
| `OpenMenuToolServiceImpl` | `getMenuComponent` | 菜单导航 |
| `DeptBizToolServiceImpl` | `createDeptWithAdmin` / `batchCreateDepts` | 业务聚合工具 |
| `Nl2SqlToolServiceImpl` | `nl2SqlQuery` | 自然语言转 SQL 查询 |

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

**完整执行链路**：用户消息 → Feign 检索 kbType=20 知识库 → 标签匹配找出相似 KbDocument → 获取 getKnowledgeId 去向量库查询 → Reranker 重排序 → 返回 Schema → LLM 生成 SQL → 安全校验 → 执行 → 结果摘要

```
用户消息
  │
  ├─ 知识库检索（Feign 调用 chat 服务，kbType=20）
  │   ├─ chat 服务端查询 kbType=20 的知识库
  │   ├─ 将用户消息与知识库内容的标签（tags）反向匹配，找出所有相似的 KbDocument
  │   ├─ 从匹配的 KbDocument 获取 getKnowledgeId()
  │   ├─ 携带 knowledgeId 单重过滤去向量库查询
  │   └─ Reranker 重排序后返回最相关的表结构Schema
  │
  ├─ SQL 生成（sqlChatClient）
  │   └─ 基于 Schema + 用户问题 + 示例 + 约束规则
  │       ├─ 强制工具调用：nl2SqlQuery 声明为"任何需要数据/统计/列表的问题必须优先调用"
  │       ├─ 系统表查询示例：sys_user / sys_dept / sys_post / sys_role 直接给出可用的 SQL 模板
  │       ├─ LIKE 模糊约束：文本字段必须用 LIKE '%关键词%'，禁止 = 精确匹配
  │       ├─ 用户查询须同时匹配 user_name（账号）+ nick_name（昵称）
  │       ├─ 岗位查询须同时匹配 post_name + post_code
  │       ├─ 角色查询须同时匹配 role_name + role_key
  │       └─ 简称也要用 LIKE 匹配完整名称（如 '研发' → 匹配 '研发部门'）
  │
  ├─ 安全校验
  │   ├─ 表名白名单校验（防止 LLM 幻觉出不存在的表）
  │   └─ JSqlParser AST 校验（只允许 SELECT，拒绝注入/多语句）
  │
  ├─ LIMIT 保护（自动添加 LIMIT 100，聚合查询除外）
  │
  ├─ 自我修正（执行失败携带错误信息重试 1 次）
  │
  └─ 远程执行（Feign 调用 mall-system，SqlQueryRequest DTO 封装）
      ├─ 修复：Feign StringHttpMessageConverter 默认 ISO-8859-1 编码
      │        导致 SQL 中的中文被转义为 '?' 的问题
      └─ 结果摘要
```

**关键实现方法**（`Nl2SqlToolServiceImpl`）：

| 方法 | 职责 |
| :--- | :--- |
| `retrieveSchema()` | 步骤1-4：Feign 调用 chat 服务的 RAG 检索接口（kbType=20），获取表结构 Schema |
| `generateValidateAndExecute()` | 步骤5：生成SQL → JSqlParser安全校验 → 表名白名单校验 → 强制LIMIT → 执行（含自我修正重试） |
| `extractTableNames()` | 从 Schema 中提取 `CREATE TABLE` 表名，构成白名单 |
| `findMissingTables()` / `collectTableNames()` | 用 JSqlParser 递归收集 SQL 引用的表名（含 JOIN/子查询/UNION），校验是否在白名单内 |
| `wrapResult()` | 封装查询结果（generatedSql + result + rowCount + summary） |

**安全校验双层防线**：

| 防线 | 说明 |
| :--- | :--- |
| 表名白名单 | 从 Schema 提取合法表名，SQL 引用了白名单外的表名 → 拒绝并触发 LLM 自我修正 |
| JSqlParser AST | 精确识别语句类型，只允许 SELECT，自动拒绝 INSERT/UPDATE/DELETE/DROP 等 |
| LIMIT 保护 | 自动添加 `LIMIT 100`（聚合查询除外），防止查询过载 |

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
        streamable-http:
          connections:
            gateway:                   # 内部 MCP 网关（聚合 mall-ai-mcp-server）
              url: http://localhost:9999
            mcp-echarts:               # 外部 ECharts MCP（自部署服务）
              url: http://114.132.102.8:2001
              endpoint: /mcp
```

**关键类**：`MallAiMcpGatewayApplication`、`GatewayController`、`WebClientConfig`

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
| `MessageChatMemoryAdvisor` | 处理近期上下文的读/写切面 | 负责调用 ChatMemory |
| `VectorStoreChatMemoryAdvisor` | **自定义**：长期语义记忆（userId 跨会话检索 + 异步写入 + upsert 合并） | 实现 BaseChatMemoryAdvisor，调用 smallChatClient 提取/合并记忆 |
| `RagContextQueryAdvisor` | **自定义**：知识库上下文查询（RAG 检索 + 注入系统提示词 + 推送 rag_retrieve 事件） | 参考 VectorStoreChatMemoryAdvisor 模式 |
| `FullHistoryChatMemoryAdvisor` | **自定义**：全量聊天记录入库 + 工具调用事件推送 | 流式拦截 ToolCall 事件 |
| `ReturnDirectChatMemoryAdvisor` | **自定义**：拦截 returnDirect 工具结果 | 单独入库，不经过 LLM |
| `ToolSearchToolCallingAdvisor` | 工具动态检索顾问 | 模型按需获取工具 |
| `SimpleLoggerAdvisor` | 请求/响应日志 | order=4 |

### 5.2 存储与记忆

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `RedisCachedAndMysqlMemoryRepository` | **自定义**：Redis+MySQL 双层存储 | 实现 ChatMemoryRepository 接口 |
| `smallChatClient` | **概述小模型** ChatClient Bean | 记忆提取/合并、会话标题生成（替代原 titleChatClient / compressChatClient） |
| `ToolDataCacheService` | 工具大数据 Redis 缓存 | dataId 机制，避免撑爆 LLM 上下文 |

### 5.3 RAG 检索

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `RagContextQueryAdvisor` | **自定义**：知识库上下文查询 Advisor，在请求链路内执行三步检索并注入系统提示词 | 参考 VectorStoreChatMemoryAdvisor 模式，推送 rag_retrieve 事件 |
| `RagRetrieveContextService` | 三步检索：先查 tag 再执行（前置检查→反向匹配→内存模糊匹配→全量降级）→ knowledgeId 单重过滤向量检索 → Reranker 重排序 | 供外部 API（KbRagRetrieveApi）和 NL2SQL 工具（kbType=20）调用 |
| `RerankerService` | 调用外部 Reranker 模型重排序 | 降级机制 |
| `KbDocumentServiceImpl` | 文档解析+切片+向量化 | md 直读 / 图片走本地 MinerU-OCR / PDF/Word 走远程 MinerU |
| `MinerUService` | MinerU 远程接口调用 | PDF/Word 解析，vlm 模式，轮询获取结果 |
| `minerUChatClient` | 本地 MinerU-OCR 视觉模型 Bean | 图片 OCR，opendatalab/MinerU2.5-Pro-2605-1.2B |

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
| `McpServerConfig` | MCP Server 配置 | 工具注册 + sqlChatClient Bean |

### 5.6 MCP 网关服务

| 组件/类名 | 职责描述 | 备注 |
| :--- | :--- | :--- |
| `MallAiMcpGatewayApplication` | 网关启动类 | 基于 spring-ai-alibaba-starter-mcp-gateway |
| `GatewayController` | 工具列表查询接口 | `GET /api/gateway/tools` 列出所有聚合工具 |
| `WebClientConfig` | WebClient 配置 | 异步 HTTP 客户端 |
| `WebClientStreamableHttpTransport` | MCP 传输层 | Streamable HTTP 传输实现 |

---

## 6. 配置参考

### 6.1 mall-ai-chat (bootstrap.yml)

```yaml
spring:
  ai:
    model:
      chat: openai
      embedding: openai
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        model: qwen3.7-flash-2026-07-15
      embedding:
        model: Qwen3-Embedding-4B-Q8_0
        base-url: http://127.0.0.1:8889/v1
    mcp:
      client:
        enabled: true
        type: ASYNC
        streamable-http:
          connections:
            gateway:
              url: http://localhost:9999      # 通过网关访问 MCP Server
            mcp-echarts:
              url: http://114.132.102.8:2001  # 自部署 ECharts MCP
              endpoint: /mcp
    vectorstore:
      weaviate:
        host: 114.132.102.8:18080
        scheme: http
        api-key: b251055070805a857b31dd014d40b727dd6a23714ea1bf66

chat-memory:
  max-messages: 4                            # 窗口记忆条数

vectorstore:
  enabled: true                              # 开启全局向量功能
  chat-memory-default-topk: 1               # 向量记忆检索条数
  compression-threshold: 4                   # 压缩阈值
  weaviate:
    knowledge-object-class: KnowledgeBase
    chat-memory-object-class: ConversationHistory
    tool-index-object-class: ToolIndex

smallmodel:                                  # 概述小模型（记忆提取/合并 + 会话标题生成）
  base-url: https://apihub.agnes-ai.com/v1
  api-key: sk-xxx
  model: agnes-2.5-flash

reranker:
  enabled: false                             # 重排序开关
  base-url: http://127.0.0.1:8887
  top-n: 3

mineru:
  base-url: https://mineru.net               # 远程 MinerU 接口（PDF/Word 解析）
  model-version: vlm                         # vlm(推荐) / pipeline / MinerU-HTML
  poll-interval-ms: 2000                     # 轮询间隔
  poll-max-attempts: 120                     # 最大轮询次数（约 4 分钟）
  vl:
    base-url: http://127.0.0.1:8890/v1       # 本地 MinerU-OCR 视觉模型（图片 OCR）
    api-key: 123456
    model: opendatalab/MinerU2.5-Pro-2605-1.2B

ai:
  tool:
    cache:
      threshold: 2000                        # 工具数据缓存阈值
      ttl-hours: 1                           # 缓存过期时间
```

### 6.2 mall-ai-mcp-server (bootstrap.yml)

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
        streamable-http:
          mcp-endpoint: /mcp
    alibaba:
      mcp:
        nacos:
          server-addr: 114.132.102.8:8848
          namespace: public
          username: nacos
          password: nacos
```

### 6.3 mall-ai-mcp-gateway (bootstrap.yml)

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
```

---

## 7. 注意事项与最佳实践

### 7.1 向量库成本与清理（高危）

*   **存储成本**：向量库不仅存文本，还存高维浮点数组，磁盘占用是 MySQL 的 10 倍以上。
*   **必须清理**：切勿永久存储所有向量。长期记忆默认 30 天 TTL（`expireAt` 字段），检索时过滤过期记忆。
*   **数据一致性**：向量库更新是异步的，消息入库后立刻查询可能查不到（延迟问题）。
*   **记忆合并机制**：`VectorStoreChatMemoryAdvisor` 采用单条 upsert 合并（无相近 → 新增；精确相同 → 续期；语义相近 → 小模型合并），替代原批量压缩方案。

### 7.2 隐私隔离（安全）

*   **向量检索隔离**：在调用 VectorStore 搜索时，必须在 Filter 中加入 `userId`（长期记忆）或 `knowledgeId`（知识库）。否则用户 A 可能搜到用户 B 的隐私记录。
*   **推荐做法**：在写入 Document 时 metadata 强制加入 userId / knowledgeId，查询时强制过滤。
*   **长期记忆作用域**：`VectorStoreChatMemoryAdvisor` 检索表达式为 `userId = {userId} AND status = active AND expireAt > now`，缺失 userId 时兜底为 `anonymous`（生产环境应在入口处保证 userId 必传）。

### 7.3 消息顺序与偶数限制

*   AI 对话极其依赖角色顺序。窗口记忆底层强制偶数，防止截断后出现连续角色错误。
*   **开发建议**：配置 `maxMessages` 时直接填写偶数（如 4, 10, 20）。

### 7.4 事务一致性

*   MySQL 全量表与窗口表不在同一个事务中。如果全量入库成功但窗口更新失败，用户可能看到历史记录但 AI "失忆"。
*   **优化**：优先保证窗口写入成功，全量入库失败可记录日志异步重试。

### 7.5 工具调用安全

*   **NL2SQL 安全校验**：JSqlParser AST 校验（只允许 SELECT）+ 表名白名单校验（防止 LLM 幻觉出不存在的表）+ 自动 LIMIT 100。
*   **按名称查询**：所有工具不接受 ID 直传，防止模型猜测 ID 导致误操作。
*   **dataId 过期**：工具缓存数据 TTL 1小时，防止 Redis 内存泄漏。

### 7.6 SSE 流式注意事项

*   **超时控制**：设置 600 秒超时，防止大模型卡死导致连接挂死。
*   **客户端断开**：通过 `doOnCancel` 处理客户端主动断开。
*   **异常脱敏**：`onErrorResume` 捕获异常后返回通用错误信息，不暴露内部细节。
*   **事件头**：`tool_call` / `rag_retrieve` 事件显式设置 SSE `event` 头，前端按事件类型路由后解析 JSON 载荷。
