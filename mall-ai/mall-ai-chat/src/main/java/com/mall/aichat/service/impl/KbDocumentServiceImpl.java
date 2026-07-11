package com.mall.aichat.service.impl;

import com.knuddels.jtokkit.api.EncodingType;
import com.mall.aichat.domain.KbDocument;
import com.mall.aichat.domain.KbDocumentChunk;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.util.List;

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

            if (StringUtils.isEmpty(filePath)) {
                return i;
            }

            // 2. 使用 Spring AI 读取文档 (Tika 支持多种格式：PDF, Word, TXT, MD)
            TextReader reader = new TextReader(new FileSystemResource(filePath));
            List<Document> documents = reader.get();
            FileSystemResource resource = new FileSystemResource(filePath);

//            TikaDocumentParser parser = new TikaDocumentParser();
//
//            List<Document> documents;
//            try (InputStream inputStream = resource.getInputStream()) {
//                // 直接调用解析方法，传入 InputStream
//                // 内部会自动根据配置文件中的 OCR 设置进行处理
//                documents = parser.parse(inputStream);
//            }

            if (documents.isEmpty()) {
                log.warn("文件 {} 解析后内容为空，跳过向量化", filePath);
                kbDocument.setStatus(1L);
                kbDocumentMapper.updateKbDocument(kbDocument);
                return i;
            }

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

            iKbDocumentChunkService.deleteKbDocumentChunkByIds(kbDocumentChunks.stream().map(KbDocumentChunk::getId).toArray(String[]::new));
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
