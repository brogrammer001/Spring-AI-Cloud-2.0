package com.mall.aichat.extractor;


import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

/**
 * Markdown / 纯文本文档提取器
 * <p>
 * .md   -> 按标题/代码块等结构边界切分为多个 Document（含元数据）
 * .txt  -> 复用同一解析器，按段落切分（Markdown 语法对其是宽松兼容的）
 */
@Component
public class MarkdownAndTextExtractor implements Extractor {

    @Override
    public boolean supports(String filename) {
        if (filename == null) {
            return false;
        }
        String name = filename.toLowerCase();
        return name.endsWith(".md") || name.endsWith(".markdown") || name.endsWith(".txt");
    }

    @Override
    public String extract(FileSystemResource resource, String filename) throws Exception {
        return Files.readString(resource.getFile().toPath());
    }
}
