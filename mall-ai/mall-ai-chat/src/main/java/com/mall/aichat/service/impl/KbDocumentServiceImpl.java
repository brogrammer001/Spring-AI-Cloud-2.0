package com.mall.aichat.service.impl;

import com.knuddels.jtokkit.api.EncodingType;
import com.mall.aichat.config.MinerUService;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Resource(name = "knowledgeVectorStore")
    private VectorStore vectorStore;

    @Autowired
    private IKbDocumentChunkService iKbDocumentChunkService;

    @Autowired
    private RemoteFileService remoteFileService;

    @Resource(name = "minerUChatClient")
    private ChatClient minerUChatClient;

    @Resource(name = "reparsingChatClient")
    private ChatClient reparsingChatClient;

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
    public int insertKbDocument(KbDocument kbDocument) {
        int i = 0;
        try {
            kbDocument.setCreateTime(DateUtils.getNowDate());
            kbDocument.setId(IdUtils.fastUUID());
            i = kbDocumentMapper.insertKbDocument(kbDocument);

            R<SysFile> fileR = remoteFileService.getFile(kbDocument.getFilePath());

            String filePath;
            if (fileR.getCode() == Constants.SUCCESS) {
                SysFile sysFile = fileR.getData();
                filePath = sysFile.getUrl();
            } else {
                filePath = "";
            }

            FileSystemResource fileResource = new FileSystemResource(filePath);
            String filename = fileResource.getFilename();
            boolean isImage = filename.toLowerCase().endsWith(".png") || filename.toLowerCase().endsWith(".jpg") ;

            List<Document> documents = new ArrayList<>();

            String parseDocument;
            if (isImage) {
                // 2. 使用 Spring AI 读取文档
                parseDocument = this.parseDocument(fileResource);
                parseDocument = this.cleanMinerUContent(parseDocument);
            }else {
                MinerUResult minerUResult = minerUService.parseMarkdown(fileResource);

                String markdownContent = minerUResult.markdown();

                if (minerUResult.images() != null && !minerUResult.images().isEmpty()) {
                    for (MinerUResult.ImageData imageData : minerUResult.images()) {
                        String imageText = this.parseDocument(new ByteArrayResource(imageData.data()));
                        String imagePlaceholder = "![](images/" + imageData.name() + ")";

                        // 为了避免替换文本中包含特殊字符影响格式，可以加上换行
                        String replacementText = "\n[" + imageText + "]\n";
                        markdownContent = markdownContent.replace(imagePlaceholder, replacementText);
                    }
                }

                parseDocument = this.cleanMinerUContent(markdownContent);
            }

            if (StringUtils.isEmpty(parseDocument)) {
                throw new RuntimeException("内容解析失败");
            }

            String finalParseDocument = parseDocument;
            String finalResult = reparsingChatClient.prompt()
                .user(u -> u.text(finalParseDocument))
                .call()
                .content();

            Document document = Document.builder()
                .text(finalResult)
                .metadata(Map.of(
                    "filename", kbDocument.getFileName(),
                    "knowledgeId", kbDocument.getKnowledgeId(),
                    "source", kbDocument.getFilePath()
                ))
                .build();
            documents.add(document);

            // 补充元数据
            documents.forEach(doc -> {
                doc.getMetadata().put("filename", kbDocument.getFileName());
                doc.getMetadata().put("knowledgeId", kbDocument.getKnowledgeId());
                doc.getMetadata().put("source", kbDocument.getFilePath());
            });

            // 3. 分块 (TokenTextSplitter 按 token 数量切分)
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(kbDocument.getChunkSize().intValue())                    // 目标每块约 800 tokens
                .withMinChunkSizeChars((int) (kbDocument.getChunkSize() * 0.5))            // 最小字符数阈值
                .withMinChunkLengthToEmbed(5)          // 小于此长度的碎片会被丢弃
                .withMaxNumChunks(10000)               // 单个文本最多生成多少块
                .withKeepSeparator(true)               // 保留换行等分隔符
                .withPunctuationMarks(List.of('.', '?', '!', '\n'))  // 用于在句尾截断的标点
                .withEncodingType(EncodingType.CL100K_BASE)   // 编码类型，默认即是该值
                .build();
            List<Document> chunks = splitter.apply(documents);

            // 4. 为每个切片附加 Metadata (用于后续在 Weaviate 中过滤)
            chunks.forEach(chunk -> chunk.getMetadata().put("knowledgeId", kbDocument.getKnowledgeId()));

            // 5. 批量存入 Weaviate (Spring AI 会自动调用 Embedding 模型生成向量并入库)
            vectorStore.add(chunks);

            // 6. 将切片信息同步保存到 MySQL (用于 UI 界面展示和统计)
            List<KbDocumentChunk> dbChunks = chunks.stream().map(chunk -> {
                KbDocumentChunk dbChunk = new KbDocumentChunk();
                // Spring AI 添加完后会生成 UUID 放在 metadata 的 _id 中
                dbChunk.setId(chunk.getId());
                dbChunk.setDocumentId(kbDocument.getId());
                dbChunk.setKnowledgeId(kbDocument.getKnowledgeId());
                dbChunk.setContent(chunk.getText());
                return dbChunk;
            }).toList();

            dbChunks.forEach(iKbDocumentChunkService::insertKbDocumentChunk); // 批量插入 MySQL

        } catch (Exception e) {
            kbDocument.setStatus(1L); // 失败
            kbDocumentMapper.updateKbDocument(kbDocument);
            throw new RuntimeException("文档处理失败", e);
        }

        return i;
    }

    public String cleanMinerUContent(String rawText) {
        if (rawText == null) return "";

        // 1. 针对死循环标签的强力正则清洗（在大模型处理前先止损）
        // 匹配类似 <|txt_contd_tgt|> <|txt_contd_src|> 的连续重复
        String pattern = "(<\\|txt_contd_tgt\\|>|<\\|txt_contd_src\\|>)+";
        String cleanedText = rawText.replaceAll(pattern, "");

        // 2. 简单的页码正则（大模型处理正则很贵，不如代码处理）
        // 假设页码是独立一行的数字
        cleanedText = cleanedText.replaceAll("(?m)^\\s*\\d+\\s*$", "");

        return cleanedText;
    }

    public String parseDocument(org.springframework.core.io.Resource resource) {
        String filename = resource.getFilename();
        MimeType mimeType;

        // 1. 动态推断文件的实际 MIME 类型
        try {
            if (resource instanceof FileSystemResource fileSystemResource) {
                // 如果是磁盘文件，优先使用系统底层的探测
                Path filePath = fileSystemResource.getFile().toPath();
                String contentType = Files.probeContentType(filePath);
                if (contentType != null) {
                    mimeType = MimeTypeUtils.parseMimeType(contentType);
                } else {
                    mimeType = guessMimeType(filename);
                }
            } else {
                // 如果是 ByteArrayResource 或其他内存资源，只能靠文件名推断
                mimeType = guessMimeType(filename);
            }
        } catch (IOException e) {
            // 探测失败时兜底
            mimeType = guessMimeType(filename);
        }

        // 2. 使用 Builder 构造 Media
        Media media = Media.builder()
            .mimeType(mimeType)
            .data(resource)
            .build();

        String instruction = "1";
        return minerUChatClient.prompt()
            .user(u -> u.text(instruction).media(media))
            .call()
            .content();
    }

    /**
     * 辅助方法：根据文件名后缀推断 MimeType
     */
    private MimeType guessMimeType(String filename) {
        if (filename == null) {
            return MimeTypeUtils.IMAGE_PNG; // 默认兜底
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return MimeTypeUtils.parseMimeType("application/pdf");
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MimeTypeUtils.IMAGE_JPEG;
        } else if (lower.endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        } else if (lower.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        }
        // 默认按图片处理
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
        int i = kbDocumentMapper.deleteKbDocumentByIds(ids);
        return i;
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
