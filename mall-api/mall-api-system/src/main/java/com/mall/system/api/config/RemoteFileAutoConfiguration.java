package com.mall.system.api.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import com.mall.common.core.utils.poi.ExcelImageHandler;
import com.mall.system.api.RemoteFileService;
import com.mall.system.api.handler.RemoteExcelImageHandler;

/**
 * Excel图片导入处理器
 *
 * @author mall
 */
@AutoConfiguration
@ConditionalOnBean(RemoteFileService.class)
public class RemoteFileAutoConfiguration
{
    @Bean
    public ExcelImageHandler excelImageHandler(RemoteFileService remoteFileService)
    {
        return new RemoteExcelImageHandler(remoteFileService);
    }
}
