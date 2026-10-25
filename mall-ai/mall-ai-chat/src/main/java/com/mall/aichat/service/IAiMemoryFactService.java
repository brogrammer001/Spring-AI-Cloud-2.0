package com.mall.aichat.service;

import java.util.List;
import com.mall.aichat.domain.AiMemoryFact;

/**
 * AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)Service接口
 * 
 * @author mall
 * @date 2026-10-24
 */
public interface IAiMemoryFactService 
{
    /**
     * 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param id AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)主键
     * @return AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     */
    public AiMemoryFact selectAiMemoryFactById(Long id);

    /**
     * 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)列表
     * 
     * @param aiMemoryFact AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * @return AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)集合
     */
    public List<AiMemoryFact> selectAiMemoryFactList(AiMemoryFact aiMemoryFact);

    /**
     * 新增AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param aiMemoryFact AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * @return 结果
     */
    public int insertAiMemoryFact(AiMemoryFact aiMemoryFact);

    /**
     * 修改AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param aiMemoryFact AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * @return 结果
     */
    public int updateAiMemoryFact(AiMemoryFact aiMemoryFact);

    /**
     * 批量删除AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param ids 需要删除的AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)主键集合
     * @return 结果
     */
    public int deleteAiMemoryFactByIds(Long[] ids);

    /**
     * 删除AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)信息
     * 
     * @param id AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)主键
     * @return 结果
     */
    public int deleteAiMemoryFactById(Long id);

    /**
     * 按会话ID批量删除长期记忆事实（删除会话时同步清理）
     *
     * @param conversationIds 需要删除的会话ID集合
     * @return 结果
     */
    public int deleteAiMemoryFactByConversationIds(String[] conversationIds);

    /**
     * 将选中的长期记忆事实重新同步到向量库：先删除旧向量，再按 MySQL 权威数据重建索引
     *
     * @param ids 需要同步的长期记忆主键集合
     * @return 实际重新写入向量库的条数（已删除/已过期记录会被跳过）
     */
    public int syncVectorStore(Long[] ids);
}
