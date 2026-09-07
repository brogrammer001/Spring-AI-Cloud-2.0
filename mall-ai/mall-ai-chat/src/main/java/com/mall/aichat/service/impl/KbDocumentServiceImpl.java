package com.mall.aichat.service.impl;

import com.mall.aichat.chunker.DocumentChunkPipeline;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Autowired
    private DocumentChunkPipeline documentChunkPipeline;

    /**
     * 是否压缩中文字符间的多余空格（OCR 噪声清理）。
     * 该规则会破坏 markdown 表格 / 行内代码的空格语义，结构化入库场景可关闭。
     */
    @Value("${clean.compress-chinese-space:true}")
    private boolean compressChineseSpace;

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

            // 占位文案契约校验：PdfExtractor 失败/超限时返回占位串，非空会绕过 isEmpty 判断
            if (rawContent.startsWith("[文档解析失败]")) {
                throw new RuntimeException("文档解析失败：" + rawContent);
            }

            // 内容清洗 (移除冗余标签)
            String cleanedContent = this.cleanContent(rawContent);
            if (StringUtils.isEmpty(cleanedContent)) {
                throw new RuntimeException("文档内容清洗后为空");
            }

            // 切分：结构化分块优先（标题/水平线/代码块/表格），无结构时智能切分兜底。
            // metadata（knowledgeId/filename/source/category/title）已随 Document 带上
            List<Document> chunks = documentChunkPipeline.toChunks(cleanedContent, kbDocument);
            if (chunks.isEmpty()) {
                throw new RuntimeException("文档切分结果为空");
            }

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
                // 切片继承文档的创建人，保证审计字段一致
                dbChunk.setCreateBy(kbDocument.getCreateBy());
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

        // 4. 处理 OCR 常见的多余空格，特别是中文字符之间的空格（可配置，避免破坏表格/行内代码空格语义）
        if (compressChineseSpace) {
            cleanedText = cleanedText.replaceAll("([\\u4e00-\\u9fa5])\\s+([\\u4e00-\\u9fa5])", "$1$2");
        }

        // 5. Word 物理分页标记转 markdown 水平线：配合 extract.page-break-mark=true 使用，
        //    --- 会被 MarkdownDocumentReader 的 withHorizontalRuleCreateDocument(true) 识别为切块边界
        cleanedText = cleanedText.replace("\n[分页]\n\n", "\n\n---\n\n");

        // 6. 规范化空行
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
