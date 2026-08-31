package com.mall.system.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.common.core.constant.CacheConstants;
import com.mall.common.core.constant.UserConstants;
import com.mall.common.core.exception.ServiceException;
import com.mall.common.core.text.Convert;
import com.mall.common.core.utils.StringUtils;
import com.mall.common.redis.service.RedisService;
import com.mall.system.api.domain.SysConfig;
import com.mall.system.mapper.SysConfigMapper;
import com.mall.system.service.ISysConfigService;

/**
 * 参数配置 服务层实现
 * 
 * @author mall
 */
@Service
public class SysConfigServiceImpl implements ISysConfigService
{
    /** 本地缓存过期时间：5分钟（毫秒） */
    private static final long LOCAL_CACHE_TTL = 5 * 60 * 1000L;

    /** 参数本地缓存，减少Redis网络往返，提升getInfo等高频接口响应速度 */
    private final Map<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();

    /** 本地缓存条目 */
    private static class LocalCacheEntry
    {
        final String value;
        final long expireAt;

        LocalCacheEntry(String value, long expireAt)
        {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean isExpired()
        {
            return System.currentTimeMillis() > expireAt;
        }
    }

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private RedisService redisService;

    /**
     * 项目启动时，初始化参数到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingConfigCache();
    }

    /**
     * 查询参数配置信息
     * 
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public SysConfig selectConfigById(Long configId)
    {
        SysConfig config = new SysConfig();
        config.setConfigId(configId);
        return configMapper.selectConfig(config);
    }

    /**
     * 根据键名查询参数配置信息
     * 
     * @param configKey 参数key
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey)
    {
        // 1. 先查本地缓存（无网络开销）
        LocalCacheEntry entry = localCache.get(configKey);
        if (entry != null && !entry.isExpired())
        {
            return entry.value;
        }
        // 2. 本地缓存未命中，查Redis
        String configValue = Convert.toStr(redisService.getCacheObject(getCacheKey(configKey)));
        if (StringUtils.isNotEmpty(configValue))
        {
            localCache.put(configKey, new LocalCacheEntry(configValue, System.currentTimeMillis() + LOCAL_CACHE_TTL));
            return configValue;
        }
        // 3. Redis未命中，查数据库并回填缓存
        SysConfig config = new SysConfig();
        config.setConfigKey(configKey);
        SysConfig retConfig = configMapper.selectConfig(config);
        if (StringUtils.isNotNull(retConfig))
        {
            redisService.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
            localCache.put(configKey, new LocalCacheEntry(retConfig.getConfigValue(), System.currentTimeMillis() + LOCAL_CACHE_TTL));
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 查询参数配置列表
     * 
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config)
    {
        return configMapper.selectConfigList(config);
    }

    /**
     * 新增参数配置
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(SysConfig config)
    {
        int row = configMapper.insertConfig(config);
        if (row > 0)
        {
            redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
            localCache.remove(config.getConfigKey());
        }
        return row;
    }

    /**
     * 修改参数配置
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int updateConfig(SysConfig config)
    {
        SysConfig temp = configMapper.selectConfigById(config.getConfigId());
        if (!StringUtils.equals(temp.getConfigKey(), config.getConfigKey()))
        {
            redisService.deleteObject(getCacheKey(temp.getConfigKey()));
        }

        int row = configMapper.updateConfig(config);
        if (row > 0)
        {
            redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
            localCache.remove(config.getConfigKey());
        }
        return row;
    }

    /**
     * 批量删除参数信息
     * 
     * @param configIds 需要删除的参数ID
     */
    @Override
    public void deleteConfigByIds(Long[] configIds)
    {
        for (Long configId : configIds)
        {
            SysConfig config = selectConfigById(configId);
            if (StringUtils.equals(UserConstants.YES, config.getConfigType()))
            {
                throw new ServiceException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            configMapper.deleteConfigById(configId);
            redisService.deleteObject(getCacheKey(config.getConfigKey()));
            localCache.remove(config.getConfigKey());
        }
    }

    /**
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache()
    {
        List<SysConfig> configsList = configMapper.selectConfigList(new SysConfig());
        for (SysConfig config : configsList)
        {
            redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
            localCache.put(config.getConfigKey(), new LocalCacheEntry(config.getConfigValue(), System.currentTimeMillis() + LOCAL_CACHE_TTL));
        }
    }

    /**
     * 清空参数缓存数据
     */
    @Override
    public void clearConfigCache()
    {
        Collection<String> keys = redisService.keys(CacheConstants.SYS_CONFIG_KEY + "*");
        redisService.deleteObject(keys);
        localCache.clear();
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache()
    {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfig config)
    {
        Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = configMapper.checkConfigKeyUnique(config.getConfigKey());
        if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 设置cache key
     * 
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_CONFIG_KEY + configKey;
    }
}