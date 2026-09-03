package com.mall.aichat.service.impl;

import com.mall.aichat.chunker.ChunkerFactory;
import com.mall.aichat.domain.KbDocument;
import com.mall.aichat.domain.KbDocumentChunk;
import com.mall.aichat.extractor.ExtractorFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 知识库文档 Service 业务层处理
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

    @Autowired
    private ExtractorFactory extractorFactory;

    @Autowired(required = false)
    private ChunkerFactory chunkerFactory;

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
                throw new RuntimeException("远程获取文件失败：" + (fileR.getMsg() != null ? fileR.getMsg() : "未知错误"));
            }

            String filePath = fileR.getData().getUrl();
            FileSystemResource fileResource = new FileSystemResource(filePath);
            String filename = fileResource.getFilename();

            // 提取纯文本/Markdown 内容（按文件后缀路由到对应的 Extractor 处理）
            String rawContent = extractorFactory.extract(fileResource, filename);
            if (StringUtils.isEmpty(rawContent)) {
                throw new RuntimeException("文档内容提取失败，内容为空");
            }

            // 内容清洗 (移除冗余标签)
            String cleanedContent = this.cleanContent(rawContent);

            Document document = Document.builder()
                .text(cleanedContent)
                .metadata(Map.of(
                    "filename", kbDocument.getFileName(),
                    "knowledgeId", kbDocument.getKnowledgeId(),
                    "source", kbDocument.getFilePath()
                ))
                .build();

            // 通用智能切分 (使用分块策略工厂)
            int chunkSize = kbDocument.getChunkSize() != null ? kbDocument.getChunkSize().intValue() : 500;
            List<Document> chunks = chunkerFactory.chunk(document, 
                Boolean.TRUE.equals(kbDocument.getSemanticChunking()), chunkSize);

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

    @Override
    public List<KbDocument> selectDocumentsByTags(String tags, String kbType, String status) {
        return kbDocumentMapper.selectDocumentsByTags(tags, kbType, status);
    }

    @Override
    public List<KbDocument> selectDocumentsByKbType(String kbType, String status) {
        return kbDocumentMapper.selectDocumentsByKbType(kbType, status);
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
