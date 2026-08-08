package com.mall.aichat.service.impl;

import com.knuddels.jtokkit.api.EncodingType;
import com.mall.aichat.domain.KbDocument;
import com.mall.aichat.domain.KbDocumentChunk;
import com.mall.aichat.domain.MinerUResult;
import com.mall.aichat.mapper.KbDocumentMapper;
import com.mall.aichat.service.IKbDocumentChunkService;
import com.mall.aichat.service.IKbDocumentService;
import com.mall.common.core.constant.Constants;
import com.mall.common.core.domain.R;
import com.mall.common.core.utils.DateUtils;
import com.mall.common.core.utils.StringUtils;
import com.mall.common.core.utils.uuid.IdUtils;
import com.mall.system.api.RemoteFileService;
import com.mall.system.api.domain.SysFile;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档Service业务层处理
 *
 * @author mall
 * @date 2026-07-05
 */
@Service
public class KbDocumentServiceImpl implements IKbDocumentService {
    private static final Logger log = LoggerFactory.getLogger(KbDocumentServiceImpl.class);
    @Autowired
    private KbDocumentMapper kbDocumentMapper;

    @Autowired(required = false)
    @Qualifier("knowledgeVectorStore")
    private VectorStore knowledgeVectorStore;

    @Autowired
    private IKbDocumentChunkService iKbDocumentChunkService;

    @Autowired
    private RemoteFileService remoteFileService;

    @Resource(name = "minerUChatClient")
    private ChatClient minerUChatClient;

    @Autowired
    private MinerUService minerUService;

    /**
     * 查询知识库文档
     *
     * @param id 知识库文档主键
     * @return 知识库文档
     */
    @Override
    public KbDocument selectKbDocumentById(String id) {
        return kbDocumentMapper.selectKbDocumentById(id);
    }

    /**
     * 查询知识库文档列表
     *
     * @param kbDocument 知识库文档
     * @return 知识库文档
     */
    @Override
    public List<KbDocument> selectKbDocumentList(KbDocument kbDocument) {
        return kbDocumentMapper.selectKbDocumentList(kbDocument);
    }

    /**
     * 新增知识库文档
     *
     * @param kbDocument 知识库文档
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertKbDocument(KbDocument kbDocument) {
        int i = 0;
        try {
            kbDocument.setCreateTime(DateUtils.getNowDate());
            kbDocument.setId(IdUtils.fastUUID());
            i = kbDocumentMapper.insertKbDocument(kbDocument);

            // 获取并解析文件
            R<SysFile> fileR = remoteFileService.getFile(kbDocument.getFilePath());
            if (fileR.getCode() != Constants.SUCCESS || fileR.getData() == null) {
                throw new RuntimeException("远程获取文件失败: " + (fileR.getMsg() != null ? fileR.getMsg() : "未知错误"));
            }

            String filePath = fileR.getData().getUrl();
            FileSystemResource fileResource = new FileSystemResource(filePath);
            String filename = fileResource.getFilename();

            // 提取纯文本/Markdown内容
            String rawContent = this.extractText(fileResource, filename);
            if (StringUtils.isEmpty(rawContent)) {
                throw new RuntimeException("文档内容提取失败，内容为空");
            }

            // 内容清洗 (移除冗余标签)
            String cleanedContent = this.cleanContent(rawContent);

            // 构建初始 Document
            Document document = Document.builder()
                .text(cleanedContent)
                .metadata(Map.of(
                    "filename", kbDocument.getFileName(),
                    "knowledgeId", kbDocument.getKnowledgeId(),
                    "source", kbDocument.getFilePath()
                ))
                .build();

            // 通用智能切分 (不再硬编码特定业务逻辑)
            List<Document> chunks = this.generalSmartSplit(document, kbDocument);

            // 存入向量库
            if (knowledgeVectorStore != null) {
                knowledgeVectorStore.add(chunks);
            } else {
                log.warn("VectorStore 未配置，跳过向量化步骤。");
            }

            // 8. 同步 MySQL Chunk 记录
            List<KbDocumentChunk> dbChunks = chunks.stream().map(chunk -> {
                KbDocumentChunk dbChunk = new KbDocumentChunk();
                dbChunk.setId(chunk.getId());
                dbChunk.setDocumentId(kbDocument.getId());
                dbChunk.setKnowledgeId(kbDocument.getKnowledgeId());
                dbChunk.setContent(chunk.getText());
                return dbChunk;
            }).toList();

            dbChunks.forEach(iKbDocumentChunkService::insertKbDocumentChunk);
        } catch (Exception e) {
            kbDocument.setStatus(1L); // 失败
            kbDocumentMapper.updateKbDocument(kbDocument);
            throw new RuntimeException("文档处理失败", e);
        }

        return i;
    }

    /**
     * 提取文本内容
     * 参照Dify，按文件类型路由到不同的提取器
     */
    private String extractText(FileSystemResource fileResource, String filename) throws Exception {
        String lowerFilename = filename.toLowerCase();

        if (lowerFilename.endsWith(".md")) {
            return Files.readString(fileResource.getFile().toPath());
        } else if (isImageFile(lowerFilename)) {
            return parseImageWithLLM(fileResource);
        } else {
            // 默认使用 MinerU 处理 PDF/Word 等复杂文档
            MinerUResult minerUResult = minerUService.parseMarkdown(fileResource);
            String markdownContent = minerUResult.markdown();

            // 处理文档内嵌的图片：将图片转为文本描述替换回原文
            if (minerUResult.images() != null && !minerUResult.images().isEmpty()) {
                for (MinerUResult.ImageData imageData : minerUResult.images()) {
                    String imageText = parseImageWithLLM(new ByteArrayResource(imageData.data()));
                    String imagePlaceholder = "![](images/" + imageData.name() + ")";
                    // 防止替换失败，使用正则
                    String replacementText = "\n[图片说明: " + imageText + "]\n";
                    markdownContent = markdownContent.replace(imagePlaceholder, replacementText);
                }
            }
            return markdownContent;
        }
    }

    /**
     * 规则清洗：处理明显的 OCR 噪声
     */
    private String cleanContent(String rawText) {
        if (rawText == null) return "";

        // 1. 移除 MinerU 等工具的特殊标签
        String cleanedText = rawText.replaceAll("(<\\|txt_contd_tgt\\|>|<\\|txt_contd_src\\|>)+", "");

        // 2. 移除仅含数字的独立行 (通常是页码)
        cleanedText = cleanedText.replaceAll("(?m)^\\s*\\d+\\s*$", "");

        // 3. 【新增】移除常见的 OCR 噪声字符，如连续的乱码符号
        cleanedText = cleanedText.replaceAll("[■□▲△○●⊛※]{2,}", "");

        // 4. 【新增】处理 OCR 常见的多余空格，特别是中文字符之间的空格
        cleanedText = cleanedText.replaceAll("([\\u4e00-\\u9fa5])\\s+([\\u4e00-\\u9fa5])", "$1$2");

        // 5. 规范化空行
        cleanedText = cleanedText.replaceAll("\\n{3,}", "\n\n");

        return cleanedText.trim();
    }

    /**
     * 通用智能切分策略
     * 改进：1. 移除硬编码表判断 2. 支持配置化 3. 更好的段落保持
     */
    private List<Document> generalSmartSplit(Document document, KbDocument kbDocument) {
        // 1. 先按段落进行粗切分，保证语义完整性
        String text = document.getText();
        String separator = StringUtils.isNotEmpty(kbDocument.getChunkSeparator()) ? kbDocument.getChunkSeparator() : "\\n\\n";
        String[] paragraphs = text.split(separator); // 按自定义分段符分段，默认按空行
        List<Document> preSplitDocs = new ArrayList<>();

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (StringUtils.isNotEmpty(trimmed)) {
                preSplitDocs.add(new Document(trimmed, document.getMetadata()));
            }
        }

        // 如果粗切分后只有一段，说明是没有标准段落的文档，直接交给 Token 切分器
        if (preSplitDocs.size() <= 1) {
            preSplitDocs = List.of(document);
        }

        // 2. Token 级别细切分，防止超长
        int chunkSize = kbDocument.getChunkSize() != null ? kbDocument.getChunkSize().intValue() : 500;

        // 参考 Dify: 提供重叠度以保留上下文，虽然 Spring AI 的 TokenTextSplitter 不直接支持 overlap，
        // 但可以通过设置 minChunkSizeChars 来尽量保证完整性
        TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(chunkSize)
            .withMinChunkSizeChars((int) (chunkSize * 0.1))
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(10000)
            .withKeepSeparator(true)
            // 增加更多的标点符号切分支持
            .withPunctuationMarks(List.of('.', '?', '!', '。', '？', '！', '\n', ';', '；'))
            .withEncodingType(EncodingType.CL100K_BASE)
            .build();
        return splitter.apply(preSplitDocs);
    }

    /**
     * 调用多模态LLM解析图片
     */
    private String parseImageWithLLM(org.springframework.core.io.Resource resource) {
        try {
            MimeType mimeType = guessMimeType(resource.getFilename());
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

    private boolean isImageFile(String filename) {
        return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".gif");
    }

    private MimeType guessMimeType(String filename) {
        if (filename == null) return MimeTypeUtils.IMAGE_PNG;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return MimeTypeUtils.parseMimeType("application/pdf");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MimeTypeUtils.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MimeTypeUtils.IMAGE_GIF;
        return MimeTypeUtils.IMAGE_PNG;
    }

    /**
     * 修改知识库文档
     *
     * @param kbDocument 知识库文档
     * @return 结果
     */
    @Override
    public int updateKbDocument(KbDocument kbDocument) {
        return kbDocumentMapper.updateKbDocument(kbDocument);
    }

    /**
     * 批量删除知识库文档
     *
     * @param ids 需要删除的知识库文档主键
     * @return 结果
     */
    @Override
    public int deleteKbDocumentByIds(String[] ids) {
        for (String id : ids) {
            KbDocument kbDocument = this.selectKbDocumentById(id);
            remoteFileService.delete(kbDocument.getFilePath());

            KbDocumentChunk kbDocumentChunk = new KbDocumentChunk();
            kbDocumentChunk.setDocumentId(id);
            List<KbDocumentChunk> kbDocumentChunks = iKbDocumentChunkService.selectKbDocumentChunkList(kbDocumentChunk);
            if (!kbDocumentChunks.isEmpty()) {
                iKbDocumentChunkService.deleteKbDocumentChunkByIds(kbDocumentChunks.stream().map(KbDocumentChunk::getId).toArray(String[]::new));
            }
        }
        return kbDocumentMapper.deleteKbDocumentByIds(ids);
    }

    /**
     * 删除知识库文档信息
     *
     * @param id 知识库文档主键
     * @return 结果
     */
    @Override
    public int deleteKbDocumentById(String id) {
        return kbDocumentMapper.deleteKbDocumentById(id);
    }

    @Override
    public int deleteKbDocumentByKnowledgeIds(String[] knowledgeIds) {
        KbDocument kbDocument = new KbDocument();
        for (String knowledgeId : knowledgeIds) {
            kbDocument.setKnowledgeId(knowledgeId);
            List<KbDocument> kbDocuments = this.selectKbDocumentList(kbDocument);
            for (KbDocument document : kbDocuments) {
                remoteFileService.delete(document.getFilePath());
            }
        }

        int i = kbDocumentMapper.deleteKbDocumentByKnowledgeIds(knowledgeIds);

        iKbDocumentChunkService.deleteKbDocumentChunkByKnowledgeIds(knowledgeIds);
        return i;
    }
}
