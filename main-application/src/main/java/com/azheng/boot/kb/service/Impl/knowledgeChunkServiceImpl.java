package com.azheng.boot.kb.service.Impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.kb.controller.request.InsertChunkRequest;
import com.azheng.boot.kb.controller.request.MkdirChunkTextRequest;
import com.azheng.boot.kb.controller.request.PageQueryChunkRequest;
import com.azheng.boot.kb.controller.request.StartChunkRequest;
import com.azheng.boot.kb.controller.vo.KnowledgeChunkVO;
import com.azheng.boot.kb.dao.mapper.FileTORustFSLogMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeBaseMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeChunkMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeDocumentMapper;
import com.azheng.boot.kb.dao.po.FileToRustFSLogPO;
import com.azheng.boot.kb.dao.po.KnowledgeBasePO;
import com.azheng.boot.kb.dao.po.KnowledgeChunkPO;
import com.azheng.boot.kb.dao.po.KnowledgeDocumentPO;
import com.azheng.boot.kb.service.KnowledgeChunkService;
import com.azheng.boot.rag.chunking.ChunkOptions;
import com.azheng.boot.rag.chunking.LangChain4jSplitter;
import com.azheng.boot.rag.embedding.milvus.operation.MilvusOperations;
import com.azheng.boot.rag.embedding.milvus.operation.VectorEmbedding;
import com.azheng.boot.rag.parser.LangChain4jParser;
import com.azheng.framework.context.UserContext;
import com.azheng.framework.exception.ClientException;
import com.azheng.framework.exception.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class knowledgeChunkServiceImpl implements KnowledgeChunkService {
    @Resource
    private LangChain4jParser langChain4jParser;

    @Resource
    private LangChain4jSplitter langChain4jSplitter;

//    @Resource
//    private Langchain4jVectorEmbedding vectorEmbedding;

    @Resource
    private MilvusOperations milvusOperations;

    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private FileTORustFSLogMapper fileTORustFSLogMapper;

    @Resource
    private VectorEmbedding vectorEmbedding;



    /**
     * 执行分块：解析——>清理——>分块——>embed——>EmbeddingStore——>MySQLStore
     */
    @Override
    public void performChunking(StartChunkRequest startChunkRequest) {
        String docId = startChunkRequest.getDocId();
        ChunkOptions chunkOptions = startChunkRequest.getChunkOptions();
        KnowledgeDocumentPO knowledgeDocumentPO = knowledgeDocumentMapper.selectById(docId);
        FileToRustFSLogPO fileToRustFSLogPO = fileTORustFSLogMapper.
                selectOne(Wrappers.lambdaQuery(FileToRustFSLogPO.class).eq(FileToRustFSLogPO::getDocId,docId));
        String rustfsKey = fileToRustFSLogPO.getRustfsKey();


        //获取所属知识库对象
        Long kbId = knowledgeDocumentPO.getKbId();
        KnowledgeBasePO knowledgeBasePO = knowledgeBaseMapper.selectById(kbId);
        String collectionName = knowledgeBasePO.getCollection();
        String embeddingModel = knowledgeBasePO.getEmbeddingModel();

        Assert.notNull(knowledgeDocumentPO, ()->new ClientException("文档不能为空，请检查客户端"));
        // 1.解析
        Document document = langChain4jParser.TikaParser(rustfsKey);
        if(document == null) {
            throw new ServiceException("解析文档为空");
        }
        // 2.分块 （TODO解析文档复杂校验）
        List<TextSegment> textSegments = langChain4jSplitter.splitterToChunk(document, chunkOptions);

        try {

            // 3.将chunk转换为向量并存入Milvus数据库 (TODO 扩展多模型,当前执行耗时太长，同步阻塞)
//            List<String> strings = vectorEmbedding.batchEmbeddingVector(textSegments, collectionName);
            List<Object> primaryKeys = vectorEmbedding.embed(textSegments, collectionName);

            // 4..遍历列表，批量存储进MySQL  (TODO改为异步进行)
            List<KnowledgeChunkPO> chunkList = new ArrayList<>();

            for (int i = 0; i < textSegments.size(); i++) {
                Long milvusId = Long.parseLong(primaryKeys.get(i).toString());
                TextSegment textSegment = textSegments.get(i);
                KnowledgeChunkPO chunkPO = toPO(textSegment, kbId, Long.parseLong(docId),milvusId);
                chunkList.add(chunkPO);
            }
            // 批量插入
            knowledgeChunkMapper.insert(chunkList);

        } catch (Exception e) {
            log.error("rag-索引阶段-数据入库：分块数据持久化异常");
            throw new ServiceException("数据持久化异常");
        }
    }






    /**
     * 删除指定知识块
     * @param chunkId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(String chunkId) {
        // 1.获取知识块对象
        KnowledgeChunkPO knowledgeChunkPO = knowledgeChunkMapper.selectById(chunkId);
        Long milvusId = knowledgeChunkPO.getMilvusId();
        Long kbId = knowledgeChunkPO.getKbId();
        KnowledgeBasePO knowledgeBasePO = knowledgeBaseMapper.selectById(kbId);
        String collection = knowledgeBasePO.getCollection();
        // 2.删除MySQL数据库记录
        knowledgeChunkMapper.update(Wrappers
                .<KnowledgeChunkPO>lambdaUpdate()
                .eq(KnowledgeChunkPO::getId, chunkId)
                .set(KnowledgeChunkPO::getDel_flag,1)
        );

        // 3.删除Milvus数据库记录(暂时不删)
        // milvusOperations.deleteEntity(milvusId,collection);
    }

    /**
     * 新增单个知识块
     * @param request
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertChunk(InsertChunkRequest request) {
        Assert.notNull(request, ()->new ClientException("新增分块请求不能为空"));
        //传来的是纯文本，不用进行解析了，只用：清理-》向量化
        String chunkText = request.getChunkText();
        Long kbId = Long.parseLong(request.getKbId());
        Long docId = Long.parseLong(request.getDocId());
        KnowledgeBasePO knowledgeBasePO = knowledgeBaseMapper.selectById(kbId);
        String collection = knowledgeBasePO.getCollection();
        //转换为TextSegment
        TextSegment segment = TextSegment.from(chunkText);
        List<TextSegment> segments = Arrays.asList(segment);
        try {
            //向量化+持久存储
            List<Object> primaryKeys = vectorEmbedding.embed(segments, collection);
            Long milvusId = Long.parseLong(primaryKeys.get(0).toString());
            //存入MySQL，记录分块
            KnowledgeChunkPO knowledgeChunkPO = toPO(segment, kbId, docId, milvusId);
            knowledgeChunkMapper.insert(knowledgeChunkPO);
        } catch (Exception e) {
            log.error("新增分块：分块数据持久化异常", e);
            throw new ServiceException("数据持久化异常");
        }
    }


    /**
     * 修改知识块文本内容
     * @param request
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChunkText(MkdirChunkTextRequest request) {
        Assert.notNull(request, ()->new ClientException("请求体不能为空"));
        String chunkId = request.getChunkId();
        KnowledgeChunkPO knowledgeChunkPO = knowledgeChunkMapper.selectById(chunkId);
        KnowledgeBasePO knowledgeBasePO = knowledgeBaseMapper.selectById(knowledgeChunkPO.getKbId());
        //1.数据库MySQL层更新
        knowledgeChunkPO.setChunkText(request.getChunkText());
        knowledgeChunkMapper.updateById(knowledgeChunkPO);

        //2.Milvus层更新：向量+字符
        milvusOperations.updateEntity(knowledgeChunkPO.getMilvusId(),knowledgeBasePO.getCollection(),request.getChunkText());
    }


    /**
     * 分页查询分块列表
     * @param request
     * @return
     */
    @Override
    public IPage<KnowledgeChunkVO> pageQuery(PageQueryChunkRequest request) {
        Assert.notNull(request, "请求不能为空");
        String docId = StrUtil.trimToNull(request.getDocId());
        String enabled = request.getEnabled();
        String searchText = StrUtil.trimToNull(request.getChunkText());
        QueryWrapper<KnowledgeChunkPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("doc_id", Long.parseLong(docId));
        queryWrapper.eq("del_flag", 0);
        if(enabled != null ){
            queryWrapper.eq("enabled", Integer.parseInt(enabled));
        }
        if(StrUtil.isNotBlank(searchText) || searchText != null) {
            queryWrapper.like("chunk_text", searchText);
        }
        queryWrapper.orderByAsc("id","update_time");

        Page<KnowledgeChunkPO> page = new Page<>(request.getCurrent(), request.getSize());
        Page<KnowledgeChunkPO> result = knowledgeChunkMapper.selectPage(page, queryWrapper);
        KnowledgeChunkVO knowledgeChunkVO = new KnowledgeChunkVO();
        BeanUtils.copyProperties(result, knowledgeChunkVO);
        return result.convert(this::toVO);
    }

    private KnowledgeChunkVO toVO(KnowledgeChunkPO knowledgeChunkPO) {
        return KnowledgeChunkVO.builder()
                .id(knowledgeChunkPO.getId().toString())
                .ChunkIndex(knowledgeChunkPO.getChunkIndex())
                .ChunkText(knowledgeChunkPO.getChunkText())
                .enabled(knowledgeChunkPO.getEnabled())
                .charCount(knowledgeChunkPO.getCharCount())
                .tokenCount(knowledgeChunkPO.getTokenCount())
                .updateTime(knowledgeChunkPO.getUpdateTime())
                .build();
    }

    private KnowledgeChunkPO toPO(TextSegment textSegment, Long kbId, Long docId, Long milvusId) {
        Metadata metadata = textSegment.metadata();
        KnowledgeChunkPO chunkPO = KnowledgeChunkPO.builder()
                .kbId(kbId)
                .docId(docId)
                .milvusId(milvusId)
                .chunkIndex(metadata.getInteger("index"))
                .chunkText(textSegment.text())
                .charCount(textSegment.text().length())
                .tokenCount(estimateTokenCount(textSegment.text()))
                .createBy(UserContext.getUsername())
                .build();
        return chunkPO;
    }

    /**
     * 估算文本token数
     */
    public Integer estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int cjkCharCount = 0;
        int otherCharCount = 0;

        for (char c : text.toCharArray()) {
            if (isCJKCharacter(c)) {
                cjkCharCount++;
            } else {
                otherCharCount++;
            }
        }

        // 其他字符向上取整：(a + b - 1) / b
        int otherTokens = (otherCharCount + 3) / 4;
        return cjkCharCount + otherTokens;
    }
    /**
     * 判断是否为中日韩字符（覆盖主要 Unicode 范围）
     */
    private boolean isCJKCharacter(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)
                || (c >= 0x3400 && c <= 0x4DBF)   // 扩展区 A
                || (c >= 0x3040 && c <= 0x30FF)   // 日文假名
                || (c >= 0xAC00 && c <= 0xD7AF);  // 韩文谚文
    }
}
