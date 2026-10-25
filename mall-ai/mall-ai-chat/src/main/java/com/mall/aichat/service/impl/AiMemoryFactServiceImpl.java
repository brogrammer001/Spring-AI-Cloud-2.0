package com.mall.aichat.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.mall.aichat.constant.ChatConstants;
import com.mall.common.core.exception.ServiceException;
import com.mall.common.core.utils.DateUtils;
import com.mall.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.mall.aichat.mapper.AiMemoryFactMapper;
import com.mall.aichat.domain.AiMemoryFact;
import com.mall.aichat.service.IAiMemoryFactService;

/**
 * AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)Service业务层处理
 * 
 * @author mall
 * @date 2026-10-24
 */
@Service
public class AiMemoryFactServiceImpl implements IAiMemoryFactService 
{
    private static final Logger log = LoggerFactory.getLogger(AiMemoryFactServiceImpl.class);

    @Autowired
    private AiMemoryFactMapper aiMemoryFactMapper;

    /** 长期记忆向量库（MySQL 为权威存储，此库仅作检索索引，未启用时为 null） */
    @Autowired(required = false)
    @Qualifier("conversationVectorStore")
    private VectorStore conversationVectorStore;

    /**
     * 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param id AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)主键
     * @return AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     */
    @Override
    public AiMemoryFact selectAiMemoryFactById(Long id)
    {
        return aiMemoryFactMapper.selectAiMemoryFactById(id);
    }

    /**
     * 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)列表
     * 
     * @param aiMemoryFact AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * @return AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     */
    @Override
    public List<AiMemoryFact> selectAiMemoryFactList(AiMemoryFact aiMemoryFact)
    {
        return aiMemoryFactMapper.selectAiMemoryFactList(aiMemoryFact);
    }

    /**
     * 新增AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param aiMemoryFact AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * @return 结果
     */
    @Override
    public int insertAiMemoryFact(AiMemoryFact aiMemoryFact)
    {
        aiMemoryFact.setCreateTime(DateUtils.getNowDate());
        return aiMemoryFactMapper.insertAiMemoryFact(aiMemoryFact);
    }

    /**
     * 修改AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param aiMemoryFact AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * @return 结果
     */
    @Override
    public int updateAiMemoryFact(AiMemoryFact aiMemoryFact)
    {
        aiMemoryFact.setUpdateTime(DateUtils.getNowDate());
        return aiMemoryFactMapper.updateAiMemoryFact(aiMemoryFact);
    }

    /**
     * 批量删除AI长期记忆事实：先删向量索引，再删 MySQL 权威记录
     *
     * @param ids 需要删除的AI长期记忆事实主键
     * @return 结果
     */
    @Override
    public int deleteAiMemoryFactByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        List<String> vectorIds = Arrays.stream(ids)
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .collect(Collectors.toList());
        // 先清理向量库：失败则中止删除，避免 MySQL 记录已删但向量仍被检索到
        if (conversationVectorStore != null && !vectorIds.isEmpty())
        {
            try
            {
                conversationVectorStore.delete(vectorIds);
            }
            catch (Exception e)
            {
                log.error("长期记忆向量删除失败, ids={}", Arrays.toString(ids), e);
                throw new ServiceException("长期记忆向量删除失败：" + e.getMessage());
            }
        }
        return aiMemoryFactMapper.deleteAiMemoryFactByIds(ids);
    }

    /**
     * 删除AI长期记忆事实信息（同步清理向量库对应数据）
     *
     * @param id AI长期记忆事实主键
     * @return 结果
     */
    @Override
    public int deleteAiMemoryFactById(Long id)
    {
        if (id == null)
        {
            return 0;
        }
        return deleteAiMemoryFactByIds(new Long[] { id });
    }

    /**
     * 按会话ID批量删除长期记忆事实（删除会话时同步清理）
     *
     * @param conversationIds 需要删除的会话ID集合
     * @return 结果
     */
    @Override
    public int deleteAiMemoryFactByConversationIds(String[] conversationIds)
    {
        return aiMemoryFactMapper.deleteAiMemoryFactByConversationIds(conversationIds);
    }

    /**
     * 将选中的长期记忆事实重新同步到向量库：先删除旧向量，再按 MySQL 权威数据重建索引
     *
     * @param ids 需要同步的长期记忆主键集合
     * @return 实际重新写入向量库的条数
     */
    @Override
    public int syncVectorStore(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        if (conversationVectorStore == null)
        {
            throw new ServiceException("向量库未启用，无法同步长期记忆");
        }

        List<AiMemoryFact> facts = aiMemoryFactMapper.selectAiMemoryFactByIds(ids);
        long now = System.currentTimeMillis();
        List<AiMemoryFact> validFacts = facts.stream()
            .filter(fact -> ChatConstants.STATUS_ACTIVE.equals(fact.getStatus()))
            .filter(fact -> fact.getValidUntil() == null || fact.getValidUntil().getTime() > now)
            .filter(fact -> StringUtils.isNotBlank(fact.getContent()))
            .collect(Collectors.toList());

        List<String> vectorIds = Arrays.stream(ids)
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .collect(Collectors.toList());
        List<Long> validIds = validFacts.stream()
            .map(AiMemoryFact::getId)
            .collect(Collectors.toList());

        try
        {
            // 1. 先删除选中记录在向量库中的旧数据
            if (!vectorIds.isEmpty())
            {
                conversationVectorStore.delete(vectorIds);
            }
            // 2. 再将有效数据按 MySQL 内容重新写入向量库
            if (!validFacts.isEmpty())
            {
                List<Document> documents = validFacts.stream()
                    .map(this::toMemoryDocument)
                    .collect(Collectors.toList());
                conversationVectorStore.write(documents);
            }
            // 3. 标记同步成功
            if (!validIds.isEmpty())
            {
                aiMemoryFactMapper.updateVectorSyncedByIds(validIds.toArray(new Long[0]), 1);
            }
            return validFacts.size();
        }
        catch (Exception e)
        {
            log.error("长期记忆向量同步失败, ids={}", Arrays.toString(ids), e);
            if (!validIds.isEmpty())
            {
                aiMemoryFactMapper.updateVectorSyncedByIds(validIds.toArray(new Long[0]), 0);
            }
            throw new ServiceException("长期记忆向量同步失败：" + e.getMessage());
        }
    }

    /**
     * 按 advisor 的元数据契约构建向量文档，保证重建后检索/排序行为一致
     */
    private Document toMemoryDocument(AiMemoryFact fact)
    {
        long now = System.currentTimeMillis();
        long expireAt = fact.getValidUntil() != null
            ? fact.getValidUntil().getTime()
            : now + ChatConstants.CHAT_MEMORY_NEVER_EXPIRE_DURATION_MS;
        Map<String, Object> metadata = new HashMap<>(16);
        metadata.put(ChatConstants.CHAT_MEMORY_USER_ID, fact.getUserId());
        metadata.put(ChatConstants.CHAT_MEMORY_CONVERSATION_ID,
            StringUtils.isNotBlank(fact.getConversationId()) ? fact.getConversationId() : "");
        metadata.put(ChatConstants.CHAT_MEMORY_TYPE,
            StringUtils.isNotBlank(fact.getFactType()) ? fact.getFactType() : "fact");
        metadata.put(ChatConstants.CHAT_MEMORY_MESSAGE_TYPE, "user");
        metadata.put(ChatConstants.CHAT_MEMORY_STATUS, ChatConstants.STATUS_ACTIVE);
        metadata.put(ChatConstants.CHAT_MEMORY_INGESTED_AT,
            fact.getCreateTime() != null ? fact.getCreateTime().getTime() : now);
        metadata.put(ChatConstants.CHAT_MEMORY_EXPIRE_AT, expireAt);
        metadata.put(ChatConstants.CHAT_MEMORY_IMPORTANCE,
            fact.getImportance() != null
                ? fact.getImportance().doubleValue()
                : ChatConstants.CHAT_MEMORY_DEFAULT_IMPORTANCE);
        return Document.builder()
            .id(String.valueOf(fact.getId()))
            .text(fact.getContent())
            .metadata(metadata)
            .build();
    }
}
