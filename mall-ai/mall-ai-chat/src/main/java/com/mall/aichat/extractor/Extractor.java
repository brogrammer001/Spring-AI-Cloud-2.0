package com.mall.aichat.extractor;

import org.apache.poi.ooxml.extractor.ExtractorFactory;
import org.springframework.core.io.FileSystemResource;

/**
 * 文档内容提取器接口
 * <p>
 * 所有文件类型提取器（PDF/Word/Excel/Markdown/TXT/图片）都实现此接口，
 * 由 {@link ExtractorFactory} 根据文件后缀路由到对应的实现类处理。
 *
 * @author mall
 */
public interface Extractor {

    /**
     * 判断该提取器是否支持指定文件类型
     *
     * @param filename 文件名（含后缀）
     * @return 是否支持
     */
    boolean supports(String filename);

    /**
     * 提取文件内容为纯文本/Markdown
     *
     * @param resource 文件资源
     * @param filename 文件名（含后缀）
     * @return 提取出的文本内容
     * @throws Exception 提取失败时抛出
     */
    String extract(FileSystemResource resource, String filename) throws Exception;
}