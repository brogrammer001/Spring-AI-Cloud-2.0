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
                你是一个精通MySQL的专家。根据自然语言问题和提供的表结构，生成正确的SQL查询语句。
                规则：
                1. 只生成 SELECT 语句，严禁修改数据。
                2. 多表查询：根据字段名语义（例如 user_id 关联 id）使用 JOIN 或 WHERE 进行关联。
                3. 默认限制：除非用户要求所有数据，否则末尾必须加 LIMIT 100。
                4. 输出格式：直接输出SQL语句，不要包含 markdown 标记（```sql）。
                """)
            .build();
    }
}
