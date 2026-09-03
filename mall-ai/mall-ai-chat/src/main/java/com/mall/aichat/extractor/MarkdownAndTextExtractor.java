package com.mall.aichat.extractor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

/**
 * Markdown 文档提取器
 * <p>
 * Markdown 文件直接读取文本内容。
 *
 * @author mall
 */
@Component
public class MarkdownAndTextExtractor implements Extractor {

    @Override
    public boolean supports(String filename) {
        return filename != null && (filename.toLowerCase().endsWith(".md") || filename.toLowerCase().endsWith(".txt"));
    }

    @Override
    public String extract(FileSystemResource resource, String filename) throws Exception {
        return Files.readString(resource.getFile().toPath());
    }
}