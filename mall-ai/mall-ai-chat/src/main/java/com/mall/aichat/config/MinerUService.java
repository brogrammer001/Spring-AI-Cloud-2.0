package com.mall.aichat.config;

import com.mall.aichat.domain.MinerUBatchResult;
import com.mall.aichat.domain.MinerUResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * MinerU解析文档配置
 */
@Component
public class MinerUService {

    private static final Logger log = LoggerFactory.getLogger(MinerUService.class);

    @Autowired
    private RestClient minerURestClient;

    @Value("${mineru.model-version}")
    private String modelVersion;

    @Value("${mineru.poll-interval-ms}")
    private long pollIntervalMs;

    @Value("${mineru.poll-max-attempts}")
    private int pollMaxAttempts;

    public MinerUBatchResult.ExtractResult parse(FileSystemResource fileResource) {
        String fileName = fileResource.getFilename();
        Objects.requireNonNull(fileName, "文件名不能为空，MinerU 靠后缀名判断文件类型");

        // 注意：这里严格按照 MinerU 官方文档的结构 {"name": "...", "data_id": "..."} 构建
        String jsonBody = String.format(
            "{\"files\":[{\"name\":\"%s\",\"data_id\":\"%s\"}],\"model_version\":\"%s\",\"enable_formula\":true,\"enable_table\":true,\"language\":\"ch\"}",
            fileName, java.util.UUID.randomUUID().toString(), modelVersion
        );

        Map<String, Object> applyResp = minerURestClient.post()
            .uri("/api/v4/file-urls/batch")
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonBody)  // 直接发送 JSON 字符串
            .retrieve()
            .body(Map.class);

        // 提取 batch_id 和 upload_url
        Integer code = (Integer) applyResp.get("code");
        if (code == null || code != 0) {
            throw new IllegalStateException("申请上传链接失败: " + applyResp.get("msg"));
        }

        Map<String, Object> data = (Map<String, Object>) applyResp.get("data");
        String batchId = (String) data.get("batch_id");
        List<String> fileUrls = (List<String>) data.get("file_urls");
        String uploadUrl = fileUrls.get(0);

        // 2. PUT 上传文件字节到预签名 URL
        try (InputStream in = fileResource.getInputStream()) {
            byte[] bytes = in.readAllBytes();

            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .timeout(Duration.ofMinutes(10))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                // 注意：移除手动设置的 Content-Length，HttpClient 会自动处理。
                // 并且不要设置任何 Content-Type，让它保持为空，与 MinerU 签名一致。
                .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status != 200 && status != 204) {
                // 重新发送请求以获取错误信息（因为上一次 discarding 忽略了 body）
                HttpResponse<String> errResp = httpClient.send(
                    HttpRequest.newBuilder()
                        .uri(URI.create(uploadUrl))
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                        .build(),
                    HttpResponse.BodyHandlers.ofString());
                throw new RuntimeException("文件上传失败，OSS 状态码: " + status + "，响应: " + errResp.body());
            }
            log.info("文件 {} 上传完成，OSS 状态码: {}", fileName, status);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }

        // 3. 轮询解析结果 (同样可以使用 Map 接收，或继续用之前的 Record)
        return pollBatchResult(batchId, fileName);
    }

    // 轮询逻辑也需要稍微调整为使用 Map 解析，或者继续用 MineruBatchResult record
    // 为保险起见，这里也改用 Map 解析
    public MinerUBatchResult.ExtractResult pollBatchResult(String batchId, String fileName) {
        for (int i = 0; i < pollMaxAttempts; i++) {
            Map<String, Object> result = minerURestClient.get()
                .uri("/api/v4/extract-results/batch/{batchId}", batchId)
                .retrieve()
                .body(Map.class);

            Integer code = (Integer) result.get("code");
            if (code == null || code != 0) {
                throw new IllegalStateException("轮询失败: " + result.get("msg"));
            }

            Map<String, Object> data = (Map<String, Object>) result.get("data");
            List<Map<String, Object>> extractResults = (List<Map<String, Object>>) data.get("extract_result");

            Map<String, Object> target = extractResults.stream()
                .filter(r -> fileName.equals(r.get("file_name")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到文件 " + fileName + " 的解析结果"));

            String state = (String) target.get("state");
            log.info("轮询第 {} 次，文件 {} 状态: {}", i + 1, fileName, state);

            if ("done".equalsIgnoreCase(state)) {
                // 组装成之前定义的 ExtractResult 返回，方便上层调用
                return new MinerUBatchResult.ExtractResult(
                    (String) target.get("file_name"),
                    (String) target.get("state"),
                    (String) target.get("err_msg"),
                    (String) target.get("full_zip_url"),
                    null // extract_progress 如果需要可以解析
                );
            }
            if ("failed".equalsIgnoreCase(state)) {
                throw new IllegalStateException("解析失败: " + target.get("err_msg"));
            }

            sleep(pollIntervalMs);
        }
        throw new IllegalStateException("轮询超时，batchId=" + batchId);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 完整流程：解析文档并直接返回 Markdown 文本
     */
    public MinerUResult parseMarkdown(FileSystemResource fileResource) {
        // 调用之前的 parse 方法获取 ExtractResult
        MinerUBatchResult.ExtractResult result = this.parse(fileResource);

        if (result == null || result.full_zip_url() == null) {
            throw new IllegalStateException("MinerU 解析失败，未获取到结果链接");
        }

        // 下载并解压 ZIP 获取 Markdown 文本
        return downloadAndExtractInMemory(result.full_zip_url());
    }

    /**
     * 下载 ZIP 到内存并提取内容
     */
    private MinerUResult downloadAndExtractInMemory(String zipUrl) {
        try {
            byte[] zipBytes = RestClient.create()
                .get()
                .uri(URI.create(zipUrl))
                .retrieve()
                .body(byte[].class);

            if (zipBytes == null || zipBytes.length == 0) {
                throw new RuntimeException("下载结果ZIP失败，内容为空");
            }

            String markdownContent = "";
            List<MinerUResult.ImageData> images = new ArrayList<>();

            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        String name = entry.getName();
                        byte[] fileBytes = zis.readAllBytes();

                        if (name.equals("full.md")) {
                            markdownContent = new String(fileBytes, StandardCharsets.UTF_8);
                        } else if (name.startsWith("images/") || name.contains("/images/")) {
                            String fileName = name.substring(name.lastIndexOf("/") + 1);
                            String mimeType = guessMimeType(fileName);
                            images.add(new MinerUResult.ImageData(fileName, fileBytes, mimeType));
                        }
                    }
                }
            }

            log.info("MinerU 解析完成，提取到 Markdown 文本和 {} 张图片", images.size());
            return new MinerUResult(markdownContent, images);

        } catch (IOException e) {
            throw new RuntimeException("下载或解析 MinerU 结果 ZIP 失败", e);
        }
    }

    private String guessMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
