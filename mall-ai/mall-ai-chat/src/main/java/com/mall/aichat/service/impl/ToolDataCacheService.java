package com.mall.aichat.service.impl;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具大数据缓存服务
 * 当工具返回的数据较大时，存入Redis生成dataId，避免撑爆LLM上下文
 */
@Service
public class ToolDataCacheService {

    private static final Logger log = LoggerFactory.getLogger(ToolDataCacheService.class);

    /** Redis Key 前缀，加上项目前缀防止冲突 */
    private static final String CACHE_KEY_PREFIX = "mall:ai:tool:data:";

    /** 正则预编译，避免每次调用重新编译 */
    private static final Pattern DATA_ID_PATTERN = Pattern.compile("dataId:([a-f0-9]{32})");

    @Value("${ai.tool.cache.threshold}")
    private int cacheThreshold;

    @Value("${ai.tool.cache.ttl-hours}")
    private long cacheTtlHours;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 如果结果较大，缓存到Redis并返回dataId引用
     *
     * @param toolName 工具名称
     * @param result   工具返回的原始结果
     * @return 如果被缓存，返回 "dataId:xxx"；否则返回原始结果
     */
    public String cacheIfLarge(String toolName, String result) {
        if (result == null || result.length() <= cacheThreshold) {
            return result;
        }

        try {
            String dataId = UUID.randomUUID().toString().replace("-", "");
            String key = CACHE_KEY_PREFIX + dataId;
            redisTemplate.opsForValue().set(key, result, cacheTtlHours, TimeUnit.HOURS);
            log.info("工具[{}]返回数据较大({}字符)，已缓存为 dataId={}", toolName, result.length(), dataId);
            return "dataId:" + dataId;
        } catch (Exception e) {
            log.error("缓存工具数据失败，返回原始结果: {}", e.getMessage(), e);
            return result;
        }
    }

    /**
     * 根据dataId从Redis获取缓存的工具数据
     *
     * @param dataId 数据ID
     * @return 缓存的原始数据，不存在则返回null
     */
    public String retrieve(String dataId) {
        try {
            String key = CACHE_KEY_PREFIX + dataId;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取缓存工具数据失败, dataId={}: {}", dataId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从输入JSON中查找并替换 dataId 引用为真实数据
     * 匹配格式：dataId:xxxxx
     *
     * @param toolInput 工具输入JSON字符串
     * @return 替换后的输入JSON
     */
    public String resolveDataIds(String toolInput) {
        if (toolInput == null || !toolInput.contains("dataId:")) {
            return toolInput;
        }

        try {
            Matcher matcher = DATA_ID_PATTERN.matcher(toolInput);
            StringBuilder sb = new StringBuilder();

            while (matcher.find()) {
                String dataId = matcher.group(1);
                String cachedData = retrieve(dataId);
                if (cachedData != null) {
                    // 使用 JSON.toJSONString 保证所有控制字符、引号等被合法转义
                    // 它会在首尾加上双引号，去掉后无缝拼接到当前JSON结构中
                    String jsonEscaped = JSON.toJSONString(cachedData);
                    if (jsonEscaped.length() >= 2 && jsonEscaped.startsWith("\"") && jsonEscaped.endsWith("\"")) {
                        jsonEscaped = jsonEscaped.substring(1, jsonEscaped.length() - 1);
                    }
                    // 防止 $ 和 \ 导致 appendReplacement 报错
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(jsonEscaped));
                    log.info("工具输入中 dataId={} 已替换为真实数据({}字符)", dataId, cachedData.length());
                } else {
                    log.warn("工具输入中 dataId={} 未找到缓存数据", dataId);
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            log.error("解析dataId失败: {}", e.getMessage(), e);
            return toolInput;
        }
    }
}
