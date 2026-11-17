package com.mall.common.security.config;

import java.util.TimeZone;
import org.springframework.context.annotation.Bean;

/**
 * 系统配置
 *
 * @author mall
 */
public class ApplicationConfig
{
    /**
     * 时区配置
     */
    @Bean
    public tools.jackson.databind.json.JsonMapper jacksonJsonMapper(tools.jackson.databind.json.JsonMapper.Builder builder)
    {
        return builder.defaultTimeZone(TimeZone.getTimeZone("GMT+8")).build();
    }
}
