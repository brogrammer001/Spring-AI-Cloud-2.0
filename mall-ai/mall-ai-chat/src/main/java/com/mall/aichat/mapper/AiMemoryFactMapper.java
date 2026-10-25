package com.mall.aichat.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.mall.aichat.domain.AiMemoryFact;

/**
 * AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)Mapper接口
 * 
 * @author mall
 * @date 2026-10-24
 */
public interface AiMemoryFactMapper 
{
    /**
     * 查询AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param id AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)主键
     * @return AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     */
    public AiMemoryFact selectAiMemoryFactById(Long id);

    /**
     * 按主键批量查询长期记忆事实（向量同步用）
     *
     * @param ids 主键集合
     * @return 长期记忆事实集合
     */
    public List<AiMemoryFact> selectAiMemoryFactByIds(Long[] ids);

    /**
     * 批量更新向量同步标记
     *
     * @param ids          主键集合
     * @param vectorSynced 1已同步 / 0待补偿
     * @return 结果
     */
    public int updateVectorSyncedByIds(@Param("ids") Long[] ids,
                                       @Param("vectorSynced") Integer vectorSynced);

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
     * 删除AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param id AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)主键
     * @return 结果
     */
    public int deleteAiMemoryFactById(Long id);

    /**
     * 批量删除AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiMemoryFactByIds(Long[] ids);

    /**
     * 按会话ID批量删除长期记忆事实
     *
     * @param conversationIds 需要删除的会话ID集合
     * @return 结果
     */
    public int deleteAiMemoryFactByConversationIds(String[] conversationIds);
}
