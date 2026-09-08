package com.mall.chatmcp.config;

import com.mall.chatmcp.sevice.BaseToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider tools(List<BaseToolService> allTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(allTools.toArray())
            .build();
    }

    @Bean(name = "sqlChatClient")
    public ChatClient sqlChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
            .defaultSystem("""
                你是一个精通MySQL的专家。根据自然语言问题、提供的表结构（含外键关联关系）、业务语义和当前时间，生成正确的SQL查询语句。
                规则：
                1. 先判断意图类型：需要查库取数才是 QUERY；打招呼/闲聊/写作等无需查库的问题返回 CHAT；存在歧义或缺必要条件返回 CLARIFY，不要硬猜SQL。
                2. 只生成单条 SELECT 语句，严禁任何修改数据的操作。
                3. 多表查询：优先使用给定的外键 JOIN 关系进行关联，不要凭空编造关联字段。
                4. 相对时间（本月/上周/上个月/近30天等）必须以提供的数据库当前时间为准换算为具体日期范围。
                5. 提供了对话上下文时先做指代消解，改写后的完整问题写入 rewritten 字段。
                6. 默认限制：除非用户要求所有数据或为聚合统计，否则末尾必须加 LIMIT 100。
                7. 先内部推理（判断意图→指代消解→识别主表→换算时间→过滤条件→JOIN→分组→聚合），再输出结果。
                8. 输出格式：必须是合法JSON，不要用markdown代码块包裹。
                   需要查库 {"type": "QUERY", "sql": "...", "rewritten": "指代消解后的完整问题", "explanation": "一句中文描述"}；
                   无需查库 {"type": "CHAT", "reply": "说明为什么不需要查库"}；
                   需要澄清 {"type": "CLARIFY", "clarify": "需要澄清的内容"}。
                """)
            .build();
    }
}
