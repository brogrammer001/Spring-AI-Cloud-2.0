package com.mall.aichat.config;

import com.mall.aichat.domain.SysChatHistory;
import com.mall.aichat.service.ISysChatHistoryService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class VectorCompressionConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorCompressionConfig.class);

    @Resource(name = "conversationVectorStore")
    private VectorStore vectorStore;

    @Resource(name = "compressChatClient")
    private ChatClient chatClient; // 用于提取摘要/事实

    @Value("${vector-store.compression-threshold}")
    private int compressionThreshold;

    @Autowired
    private ISysChatHistoryService sysChatHistoryService;


    /**
     * 异步执行压缩，不阻塞主线程
     */
    @Async("taskExecutor") // 建议配置一个独立的线程池
    public void checkAndCompressAsync(String conversationId) {
        try {
            // 1. 查询该 conversationId 的所有向量记录
            SysChatHistory sysChatHistory = new SysChatHistory();
            sysChatHistory.setIsCompression("N");
            sysChatHistory.setConversationId(conversationId);
            List<SysChatHistory> sysChatHistories = sysChatHistoryService.selectSysChatHistoryList(sysChatHistory);

            if (sysChatHistories.size() > compressionThreshold) {
                doCompress(conversationId, sysChatHistories);
            }
        } catch (Exception e) {
            // 压缩失败不能影响主流程，仅打印日志
            log.error("压缩失败，{}", e.getMessage());
        }
    }

    private void doCompress(String conversationId, List<SysChatHistory> sysChatHistoryList) {
        // 2. 构建 Prompt 让 LLM 提取原子事实
        String historyText = sysChatHistoryList.stream()
            .map(SysChatHistory::getContent)
            .collect(Collectors.joining("\n"));

        // 3. 调用 LLM (注意：这是异步后台执行的，不卡顿)
        String factsJson = chatClient.prompt().user(historyText).call().content();

        SystemMessage systemMessage = new SystemMessage(factsJson);
        // 4. 解析 JSON 并生成新 Document (略，用 Jackson 解析)
        List<Document> newFacts = toDocuments(List.of(systemMessage), conversationId);

        // 删除旧向量
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(b.eq("conversationId", conversationId).build());

        //将压缩的内容放入向量库
        vectorStore.add(newFacts);

        //更新压缩标识
        sysChatHistoryList.stream().peek(history -> history.setIsCompression("Y")).forEach(sysChatHistoryService::updateSysChatHistory);
        log.error("压缩成功");
    }

    //转文档
    private List<Document> toDocuments(List<Message> messages, String conversationId) {
        return messages.stream()
            .filter(m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
            .map(message -> {
                message.getMetadata();
                Map<String, Object> metadata = new HashMap<>(
                    message.getMetadata());
                metadata.put("conversationId", conversationId);
                metadata.put("messageType", message.getMessageType().name());
                if (message instanceof UserMessage userMessage) {
                    return Document.builder()
                        .text(userMessage.getText())
                        .metadata(metadata)
                        .build();
                } else if (message instanceof AssistantMessage assistantMessage) {
                    return Document.builder().text(assistantMessage.getText()).metadata(metadata).build();
                }
                throw new RuntimeException("Unknown message type: " + message.getMessageType());
            })
            .toList();
    }
}
