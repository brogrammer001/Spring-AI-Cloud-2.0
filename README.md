# Spring AI 2.0 企业级 ChatBot 架构与实现指南
## 1. 核心架构概览
本系统基于 Spring AI 2.0 构建，采用 **"三层存储 + 双模记忆"** 架构，旨在平衡 AI 响应的上下文连贯性、语义检索准确性以及系统的存储成本与性能。
*   **双模记忆**：结合 `WindowChatMemory`（近期上下文）与 `VectorStoreChatMemory`（长期语义记忆）。
*   **三层存储**：Redis（热缓存） + MySQL（结构化持久化） + VectorStore（非结构化语义索引）。
---
## 2. 功能模块详细设计
### 2.1 系统全局提示词
*   **实现方式**：在构建 `ChatClient` Bean 时，通过 `defaultSystem` 统一加载。
*   **加载时机**：应用启动时加载一次，避免每次请求重复 IO。
*   **配置要点**：
    *   将 System Prompt 定义为独立的 Markdown 文件（如 `system-prompt.md`），便于版本控制和热更新。
### 2.2 会话记忆与上下文管理
#### 2.2.1 多用户会话隔离
*   **隔离维度**：`userId`（用户维度的数据权限） + `conversationId`（单次会话维度的上下文连续性）。
*   **实现机制**：
    *   当用户第一次输入内容进行请求，创建conversationId并与用户id建立关联关系
    *   通过 `自定义Advisor-ConversationInitAdvisor` 上下文传递 `ChatMemory.CONVERSATION_ID` 和 `userId`校验是否合法。
    *   在所有存储层（Redis, MySQL, VectorStore）的数据写入时，必须带上CONVERSATION_ID
#### 2.2.2 上下文窗口（近期记忆 - Window Memory）
这是 AI "正在看"的内容，决定了对话的连续性。
*   **窗口策略**：基于 `maxMessages` 滑动窗口。
    *   **逻辑**：仅保留最近的 N 条消息。达到上限时，自动移除最早的一轮对话。
    *   **奇偶校验**：为了保证“一问一答”的完整性，底层会自动将奇数 `maxMessages` 向下取整为偶数（如输入 5，实际生效 4）。
*   **存储实现（双写策略）**：
    1.  **Redis 缓存层**：
        *   **作用**：提供毫秒级的读写速度，支撑高并发会话。
        *   **策略**：Key 设置 TTL（如 7天），过期自动清理。
    2.  **MySQL 持久化层**：
        *   **作用**：作为 Redis 的持久化备份，防止 Redis 故障丢失近期上下文，当redis数据过期，可通过查询将数据缓存到redis
        *   **策略**：同步窗口数据，同样遵循滑动窗口截断逻辑。
*   **重点类**：`MessageChatMemoryAdvisor`, `MessageWindowChatMemory`, `RedisCachedAndMysqlMemoryRepository` (自定义).
#### 2.2.3 向量库全量上下文（长期记忆 - Vector Memory）
这是 AI "能想起"的内容，实现了跨会话、长期的语义检索。
*   **应用场景**：用户询问“我最初问了什么”，即使该对话不在当前窗口，AI 也能通过向量检索找到。
*   **技术栈**：本地 Embedding 模型（Qwen3-Embedding） + Weaviate（向量数据库）。
*   **检索逻辑**：基于 `VectorStoreChatMemoryAdvisor`，根据当前问题相似度检索历史。
    *   **参数**：`defaultTopK`（例如 3），即返回相似度最高的 3 条历史片段。
*   **重点类**：`VectorStoreChatMemoryAdvisor`。
#### 2.2.4 全量聊天记录（业务展示）
*   **作用**：供前端展示“历史会话列表”和“聊天详情”，支持分页、关键词搜索。
*   **存储**：MySQL 业务表（如 `sys_chat_history`）。
*   **特点**：**全量永久存储**（除非用户主动删除），不进行滑动窗口截断。
*   **重点类**：`FullHistoryChatMemory`。
---
## 3. 关键类与组件清单
| 组件/类名                             | 职责描述                                 | 备注                                      |
| :------------------------------------ | :--------------------------------------- | :---------------------------------------- |
| `ChatClient`                          | AI 交互的统一入口                        | Builder 模式构建                          |
| `MessageChatMemoryAdvisor`            | 处理近期上下文的读/写切面                | 负责调用 ChatMemory                       |
| `MessageWindowChatMemory`             | 窗口策略的具体实现                       | 管理 maxMessages 截断逻辑                 |
| `VectorStoreChatMemoryAdvisor`        | 处理长期语义检索的读切面                 | 负责调用 VectorStore 搜索                 |
| `RedisCachedAndMysqlMemoryRepository` | **自定义实现**：Redis+MySQL 双层存储     | 实现了 ChatMemoryRepository 接口          |
| `FullHistoryChatMemory`               | **自定义包装类**：协调窗口存储与全量存储 | 在写入时同时触发窗口更新和 MySQL 全量入库 |
| `VectorStore`                         | Spring AI 向量存储抽象接口               | 对接 Weaviate 等                          |
---
## 4. 注意事项与最佳实践
### 4.1 向量库成本与清理（⚠️ 高危）
*   **存储成本**：向量库不仅存文本，还存高维浮点数组（如 1536 维），磁盘占用是 MySQL 的 10 倍以上。
*   **必须清理**：**切勿永久存储所有向量**。建议设置 TTL（如 180 天），或编写定时任务清理超过 1 年的向量数据，否则检索性能会随数据量增长急剧下降。
*   **数据一致性**：向量库更新是异步的，如果消息入库后立刻查询向量库，可能查不到（延迟问题）。
### 4.2 隐私隔离（🔒 安全）
*   **向量检索隔离**：在调用 `VectorStore` 搜索时，**必须**在 Filter 中加入 `userId`。否则，用户 A 可能会搜索到用户 B 的隐私记录（例如：“帮我查一下订单”可能会搜到别人的订单号）。
    *   *推荐做法：在写入 Document 时 metadata 强制加入 userId，查询时强制过滤。*
### 4.3 System Prompt 的格式冲突
*   如果你的 System Prompt 规定了输出格式（如纯 JSON，无 Markdown），请确保：
    1.  模型遵守指令（通过 Prompt 约束）。
    2.  Spring AI 的 SSE（Server-Sent Events）配置不会破坏格式。标准的 SSE 会自动在每行前加 `data:`。如果前端需要纯 JSON 流，需调整 `produces` 或前端解析逻辑。
### 4.4 消息顺序与偶数限制
*   AI 对话极其依赖角色顺序。`MessageWindowChatMemory` 底层之所以强制偶数，是为了防止截断后出现 `User -> User` 或 `Assistant -> Assistant` 的连续角色错误。
*   **开发建议**：配置 `maxMessages` 时直接填写偶数（如 4, 10, 20），避免产生歧义。
### 4.5 事务一致性
*   MySQL 全量表（业务）与 MySQL 窗口表 可能不在同一个事务中。如果全量入库成功但窗口更新失败，用户可能看到历史记录但 AI “失忆”。
*   **优化**：在 `FullHistoryChatMemory` 中，优先保证窗口写入成功，全量入库失败可记录日志异步重试，不要阻塞主流程。