package com.mall.aichat.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * 图片提取器
 * <p>
 * 调用多模态大模型解析图片内容为文字描述。
 *
 * @author mall
 */
@Component
public class ImageExtractor implements Extractor {

    private static final Logger log = LoggerFactory.getLogger(ImageExtractor.class);

    @jakarta.annotation.Resource(name = "minerUChatClient")
    private ChatClient minerUChatClient;

    @Override
    public boolean supports(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".png")
            || lower.endsWith(".jpg")
            || lower.endsWith(".jpeg")
            || lower.endsWith(".gif");
    }

    @Override
    public String extract(FileSystemResource resource, String filename) {
        return extractImage(resource, filename);
    }

    /**
     * 调用多模态LLM解析图片（供图片文件与文档内嵌图片共用）
     */
    public String extractImage(Resource resource, String filename) {
        try {
            MimeType mimeType = guessMimeType(filename);
            Media media = Media.builder().mimeType(mimeType).data(resource).build();

            return minerUChatClient.prompt()
                .user(u -> u.text("1").media(media))
                .call()
                .content();
        } catch (Exception e) {
            log.error("图片解析失败: {}", e.getMessage());
            return "[图片解析失败]";
        }
    }

    private MimeType guessMimeType(String filename) {
        if (filename == null) return MimeTypeUtils.IMAGE_PNG;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MimeTypeUtils.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MimeTypeUtils.IMAGE_GIF;
        return MimeTypeUtils.IMAGE_PNG;
    }
}