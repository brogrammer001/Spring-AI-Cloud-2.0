package com.mall.aichat.extractor;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * 音频文件提取器
 * <p>
 * 使用 Apache Tika 提取音频文件的元数据信息，包括：
 * <ul>
 *   <li>标题 (Title)</li>
 *   <li>艺术家 (Artist)</li>
 *   <li>专辑 (Album)</li>
 *   <li>年份/日期 (Date/Year)</li>
 *   <li>时长 (Length)</li>
 *   <li>编码格式 (Encoding Format)</li>
 *   <li>比特率 (Bitrate)</li>
 *   <li>采样率 (Sample Rate)</li>
 * </ul>
 * <p>
 * 支持格式：MP3、WAV、OGG、FLAC、M4A、AAC 等常见音频格式
 * <p>
 * 输出格式为结构化的文本描述，便于后续 RAG 处理和语义检索
 *
 * @author mall
 */
@Component
public class AudioExtractor implements Extractor {

    private static final Logger log = LoggerFactory.getLogger(AudioExtractor.class);

    /** 音频常见元数据字段名称 */
    private static final String FIELD_TITLE = "dc:title";
    private static final String FIELD_ARTIST = "audition:artist";
    private static final String FIELD_ALBUM = "audio:album";
    private static final String FIELD_DATE = "dc:date";
    private static final String FIELD_YEAR = "audio:year";
    private static final String FIELD_TRACK = "audio:tracknumber";
    private static final String FIELD_GENRE = "dc:format";
    private static final String FIELD_ENCODED_BY = "tiff:Artist";
    private static final String FIELD_LENGTH = "length";
    private static final String FIELD_BITRATE = "bitrate";
    private static final String FIELD_SAMPLERATE = "samplerate";
    private static final String FIELD_ENCODING = "tika:encoding";
    
    // 已处理的字段列表
    private static final String[] PROCESSED_FIELDS = {
        FIELD_TITLE, FIELD_ARTIST, FIELD_ALBUM, FIELD_DATE, FIELD_YEAR,
        FIELD_TRACK, FIELD_GENRE, FIELD_ENCODED_BY, FIELD_LENGTH, 
        FIELD_BITRATE, FIELD_SAMPLERATE, FIELD_ENCODING,
        "filename", "content-type", "modified"
    };

    @Override
    public boolean supports(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        // 支持常见的音频格式
        return lower.endsWith(".mp3") || 
               lower.endsWith(".wav") || 
               lower.endsWith(".ogg") ||
               lower.endsWith(".flac") ||
               lower.endsWith(".m4a") ||
               lower.endsWith(".aac") ||
               lower.endsWith(".wma") ||
               lower.endsWith(".aiff") ||
               lower.endsWith(".au") ||
               lower.endsWith(".ra") ||
               lower.endsWith(".amr");
    }

    @Override
    public String extract(FileSystemResource resource, String filename) throws Exception {
        File file = resource.getFile();
        
        // 使用 Tika 提取元数据
        Metadata metadata = new Metadata();
        AutoDetectParser parser = new AutoDetectParser();
        
        try (InputStream in = new FileInputStream(file)) {
            // 只提取元数据，不需要内容处理
            parser.parse(in, null, metadata, null);
        } catch (Exception e) {
            log.warn("Tika parse error for audio: {}, will use metadata only", filename, e);
        }
        
        // 构建结构化文本输出
        StringBuilder textBuilder = new StringBuilder();
        
        // 添加文件名作为基本信息
        textBuilder.append("# ").append(filename).append("\n\n");
        
        // 添加基础元数据
        textBuilder.append("## 文件信息\n");
        appendMetadata(textBuilder, metadata, "编码者", FIELD_ENCODED_BY, "");
        appendMetadata(textBuilder, metadata, "描述", "dc:description", "无描述");
        textBuilder.append("\n");
        
        // 添加媒体属性
        textBuilder.append("## 媒体信息\n");
        appendMediaInfo(textBuilder, metadata, "标题", FIELD_TITLE, "未提供标题");
        appendMediaInfo(textBuilder, metadata, "艺术家", FIELD_ARTIST, "未知艺术家");
        appendMediaInfo(textBuilder, metadata, "专辑", FIELD_ALBUM, "未提供专辑");
        appendMediaInfo(textBuilder, metadata, "年份", FIELD_YEAR, "未知年份");
        appendMediaInfo(textBuilder, metadata, "日期", FIELD_DATE, "");
        appendMediaInfo(textBuilder, metadata, "曲目号", FIELD_TRACK, "");
        appendMediaInfo(textBuilder, metadata, "流派", FIELD_GENRE, "未知流派");
        appendAudioInfo(textBuilder, metadata, "时长", FIELD_LENGTH, "未提供时长");
        appendAudioInfo(textBuilder, metadata, "编码格式", FIELD_ENCODING, "未提供编码信息");
        appendAudioInfo(textBuilder, metadata, "比特率", FIELD_BITRATE, "未提供比特率");
        appendAudioInfo(textBuilder, metadata, "采样率", FIELD_SAMPLERATE, "未提供采样率");
        textBuilder.append("\n");
        
        // 添加所有其他元数据字段
        textBuilder.append("## 完整元数据\n");
        String[] allFields = metadata.names();
        
        // 跳过已处理的字段和一些不需要的字段
        int foundCount = 0;
        for (String field : allFields) {
            // 跳过已处理的字段
            if (isProcessedField(field)) {
                continue;
            }
            
            String value = metadata.get(field);
            if (value != null && !value.trim().isEmpty()) {
                if (foundCount > 0) {
                    textBuilder.append("\n");
                }
                textBuilder.append(field).append(": ").append(value);
                foundCount++;
            }
        }
        
        if (foundCount == 0) {
            textBuilder.append("无其他可用元数据");
        }
        
        return textBuilder.toString().trim();
    }

    /**
     * 判断是否为已处理的字段
     */
    private boolean isProcessedField(String field) {
        for (String processed : PROCESSED_FIELDS) {
            if (field.equals(processed)) {
                return true;
            }
        }
        return field.startsWith("tika:") ||
               field.startsWith("pdf:") ||
               field.startsWith("xmp:") ||
               field.startsWith("exif:");
    }

    /**
     * 追加普通元数据信息
     */
    private void appendMetadata(StringBuilder builder, Metadata metadata, 
                                String fieldName, String metadataField, String defaultValue) {
        String value = metadata.get(metadataField);
        if (value != null && !value.trim().isEmpty()) {
            builder.append(fieldName).append(": ").append(value).append("\n");
        } else if (defaultValue != null && !defaultValue.isEmpty()) {
            builder.append(fieldName).append(": ").append(defaultValue).append("\n");
        }
    }

    /**
     * 追加媒体信息（带默认值）
     */
    private void appendMediaInfo(StringBuilder builder, Metadata metadata,
                                 String fieldName, String fieldKey, String defaultValue) {
        String value = metadata.get(fieldKey);
        if (value != null && !value.trim().isEmpty()) {
            builder.append(fieldName).append(": ").append(value).append("\n");
        } else if (defaultValue != null && !defaultValue.isEmpty()) {
            builder.append(fieldName).append(": ").append(defaultValue).append("\n");
        }
    }

    /**
     * 追加音频技术信息（带默认值）
     */
    private void appendAudioInfo(StringBuilder builder, Metadata metadata,
                                 String fieldName, String fieldKey, String defaultValue) {
        String value = metadata.get(fieldKey);
        if (value != null && !value.trim().isEmpty()) {
            builder.append(fieldName).append(": ").append(value).append("\n");
        } else if (defaultValue != null && !defaultValue.isEmpty()) {
            builder.append(fieldName).append(": ").append(defaultValue).append("\n");
        }
    }
}
