package com.mall.aichat.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.mall.common.core.annotation.Excel;
import com.mall.common.core.web.domain.BaseEntity;

/**
 * AI长期记忆事实(MySQL为权威存储，Weaviate仅为检索索引，可凭本全量重建)对象 ai_memory_fact
 * 
 * @author mall
 * @date 2026-10-24
 */
public class AiMemoryFact extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键，自增；同时作为Weaviate文档id，双存储对齐依据 */
    private Long id;

    /** 归属用户ID(来自CTX_USER_ID)；匿名用户不落库；去重/隔离/管理页查询键 */
    @Excel(name = "归属用户ID(来自CTX_USER_ID)；匿名用户不落库；去重/隔离/管理页查询键")
    private String userId;

    /** Agent标识，多Agent记忆隔离；与Weaviate metadata.agentId一致；默认default */
    @Excel(name = "Agent标识，多Agent记忆隔离；与Weaviate metadata.agentId一致；默认default")
    private String agentId;

    /** 首次产生该记忆的会话ID，仅溯源用，不参与检索；可空 */
    @Excel(name = "首次产生该记忆的会话ID，仅溯源用，不参与检索；可空")
    private String conversationId;

    /** 事实分类: profile身份/preference偏好/entity关系/event事件/skill技能/fact兜底；非法值归一为fact */
    @Excel(name = "事实分类: profile身份/preference偏好/entity关系/event事件/skill技能/fact兜底；非法值归一为fact")
    private String factType;

    /** 记忆正文；入库前已脱敏(手机号/身份证/银行卡→***)；prompt约束单条≤100字，合并更新留余量 */
    @Excel(name = "记忆正文；入库前已脱敏(手机号/身份证/银行卡→***)；prompt约束单条≤100字，合并更新留余量")
    private String content;

    /** 重要性0.00~1.00：显式要求1.0/身份安全0.9/偏好0.7/一般0.3；用于画像常驻、排序加权、低分淘汰 */
    @Excel(name = "重要性0.00~1.00：显式要求1.0/身份安全0.9/偏好0.7/一般0.3；用于画像常驻、排序加权、低分淘汰")
    private BigDecimal importance;

    /** 去重指纹SHA256(userId|脱敏content)；命中即跳过向量检索+裁决直接续期；应用层查询去重(未加唯一约束以兼容多agent) */
    @Excel(name = "去重指纹SHA256(userId|脱敏content)；命中即跳过向量检索+裁决直接续期；应用层查询去重(未加唯一约束以兼容多agent)")
    private String hash;

    /** 状态: active有效/deleted已删除(裁决或用户操作)/expired已过期(淘汰任务)；软删保留记录，回表校验仅认active */
    @Excel(name = "状态: active有效/deleted已删除(裁决或用户操作)/expired已过期(淘汰任务)；软删保留记录，回表校验仅认active")
    private String status;

    /** 有效期截止=最后活跃时间+TTL；ADD/续期/UPDATE时刷新；淘汰任务扫描依据；NULL=永不过期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "有效期截止=最后活跃时间+TTL；ADD/续期/UPDATE时刷新；淘汰任务扫描依据；NULL=永不过期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date validUntil;

    /** 向量同步标记: 1已同步Weaviate/0待补偿；ReconcileJob重推依据；全量重建索引时置0重放 */
    @Excel(name = "向量同步标记: 1已同步Weaviate/0待补偿；ReconcileJob重推依据；全量重建索引时置0重放")
    private Integer vectorSynced;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(String userId) 
    {
        this.userId = userId;
    }

    public String getUserId() 
    {
        return userId;
    }

    public void setAgentId(String agentId) 
    {
        this.agentId = agentId;
    }

    public String getAgentId() 
    {
        return agentId;
    }

    public void setConversationId(String conversationId) 
    {
        this.conversationId = conversationId;
    }

    public String getConversationId() 
    {
        return conversationId;
    }

    public void setFactType(String factType) 
    {
        this.factType = factType;
    }

    public String getFactType() 
    {
        return factType;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setImportance(BigDecimal importance) 
    {
        this.importance = importance;
    }

    public BigDecimal getImportance() 
    {
        return importance;
    }

    public void setHash(String hash) 
    {
        this.hash = hash;
    }

    public String getHash() 
    {
        return hash;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setValidUntil(Date validUntil) 
    {
        this.validUntil = validUntil;
    }

    public Date getValidUntil() 
    {
        return validUntil;
    }

    public void setVectorSynced(Integer vectorSynced) 
    {
        this.vectorSynced = vectorSynced;
    }

    public Integer getVectorSynced() 
    {
        return vectorSynced;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("agentId", getAgentId())
            .append("conversationId", getConversationId())
            .append("factType", getFactType())
            .append("content", getContent())
            .append("importance", getImportance())
            .append("hash", getHash())
            .append("status", getStatus())
            .append("validUntil", getValidUntil())
            .append("vectorSynced", getVectorSynced())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
