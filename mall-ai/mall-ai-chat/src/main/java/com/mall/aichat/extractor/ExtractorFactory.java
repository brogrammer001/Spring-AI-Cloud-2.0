package com.mall.aichat.extractor;

import com.mall.common.core.utils.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档提取器工厂
 * <p>
 * 根据文件后缀自动路由到对应的 {@link Extractor} 实现类处理。
 * 新增文件类型时，只需实现 {@link Extractor} 接口并注册为 Spring Bean，
 * 工厂会自动发现并参与路由，无需修改本类。
 *
 * @author mall
 */
@Component
public class ExtractorFactory {

    private final List<Extractor> extractors;

    public ExtractorFactory(List<Extractor> extractors) {
        this.extractors = extractors;
    }

    /**
     * 根据文件名获取匹配的提取器
     *
     * @param filename 文件名（含后缀）
     * @return 匹配的提取器
     * @throws IllegalArgumentException 没有匹配的提取器时抛出
     */
    public Extractor getExtractor(String filename) {
        if (StringUtils.isEmpty(filename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return extractors.stream()
            .filter(e -> e.supports(filename))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("不支持的文件类型: " + filename));
    }

    /**
     * 根据文件名提取文件内容
     *
     * @param resource 文件资源
     * @param filename 文件名（含后缀）
     * @return 提取出的文本内容
     * @throws Exception 提取失败时抛出
     */
    public String extract(FileSystemResource resource, String filename) throws Exception {
        return getExtractor(filename).extract(resource, filename);
    }
}