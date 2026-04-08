package com.azheng.boot.kb.service.Impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.kb.controller.request.MkdirChunkTextRequest;
import com.azheng.boot.kb.controller.request.PageQueryChunkRequest;
import com.azheng.boot.kb.controller.request.StartChunkRequest;
import com.azheng.boot.kb.controller.vo.KnowledgeBaseVO;
import com.azheng.boot.kb.controller.vo.KnowledgeChunkVO;
import com.azheng.boot.kb.dao.mapper.FileTORustFSLogMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeChunkMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeDocumentMapper;
import com.azheng.boot.kb.dao.po.FileToRustFSLogPO;
import com.azheng.boot.kb.dao.po.KnowledgeBasePO;
import com.azheng.boot.kb.dao.po.KnowledgeChunkPO;
import com.azheng.boot.kb.dao.po.KnowledgeDocumentPO;
import com.azheng.boot.kb.service.KnowledgeChunkService;
import com.azheng.boot.rag.chunking.ChunkOptions;
import com.azheng.boot.rag.chunking.LangChain4jSplitter;
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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class knowledgeChunkServiceImpl implements KnowledgeChunkService {
    @Resource
    private LangChain4jParser langChain4jParser;

    @Resource
    private LangChain4jSplitter langChain4jSplitter;

    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private FileTORustFSLogMapper fileTORustFSLogMapper;

    @Override
    public void performChunking(StartChunkRequest startChunkRequest) {
        String docId = startChunkRequest.getDocId();
        ChunkOptions chunkOptions = startChunkRequest.getChunkOptions();
        KnowledgeDocumentPO knowledgeDocumentPO = knowledgeDocumentMapper.selectById(docId);
        FileToRustFSLogPO fileToRustFSLogPO = fileTORustFSLogMapper.
                selectOne(Wrappers.lambdaQuery(FileToRustFSLogPO.class).eq(FileToRustFSLogPO::getDocId,docId));
        String rustfsKey = fileToRustFSLogPO.getRustfsKey();
        Long kbId = knowledgeDocumentPO.getKbId();
        Assert.notNull(knowledgeDocumentPO, ()->new ClientException("文档不能为空，请检查客户端"));
        /// 1.解析
        Document document = langChain4jParser.TikaParser(rustfsKey);
        if(document == null) {
            throw new ServiceException("解析文档为空");
        }
        /// TODO解析文档复杂校验
        /// 2.分块
        List<TextSegment> textSegments = langChain4jSplitter.splitterToChunk(document, chunkOptions);

        /// 3.遍历列表，批量存储进MySQL
        List<KnowledgeChunkPO> chunkList = new ArrayList<>();

        // 遍历所有分块
        for (TextSegment textSegment : textSegments) {
            Metadata metadata = textSegment.metadata();
            KnowledgeChunkPO chunkPO = KnowledgeChunkPO.builder()
                    .kbId(kbId)
                    .docId(Long.parseLong(docId))
                    .chunkIndex(metadata.getInteger("index"))
                    .chunkText(textSegment.text())
                    .charCount(textSegment.text().length())
                    .tokenCount(estimateTokenCount(textSegment.text()))
                    .createBy(UserContext.getUsername())
                    .build();
            chunkList.add(chunkPO);
        }
        // 批量插入
        knowledgeChunkMapper.insert(chunkList);
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
        queryWrapper.orderByAsc("update_time");

        Page<KnowledgeChunkPO> page = new Page<>(request.getCurrent(), request.getSize());
        Page<KnowledgeChunkPO> result = knowledgeChunkMapper.selectPage(page, queryWrapper);
        KnowledgeChunkVO knowledgeChunkVO = new KnowledgeChunkVO();
        BeanUtils.copyProperties(result, knowledgeChunkVO);
        return result.convert(this::toVO);
    }

    /**
     * 修改知识块文本内容
     * @param request
     */
    @Override
    public void updateChunkText(MkdirChunkTextRequest request) {
        Assert.notNull(request, ()->new ClientException("请求体不能为空"));
        String chunkId = request.getChunkId();
        KnowledgeChunkPO knowledgeChunkPO = knowledgeChunkMapper.selectById(chunkId);
        knowledgeChunkPO.setChunkText(request.getChunkText());
        knowledgeChunkMapper.updateById(knowledgeChunkPO);
    }


    /**
     * 删除指定知识块
     * @param chunkId
     */
    @Override
    public void remove(String chunkId) {
        knowledgeChunkMapper.deleteById(chunkId);
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
        return (c >= 0x4E00 && c <= 0x9FFF)   // 中日韩统一表意文字
                || (c >= 0x3400 && c <= 0x4DBF)   // 扩展区 A
                || (c >= 0x3040 && c <= 0x30FF)   // 日文假名
                || (c >= 0xAC00 && c <= 0xD7AF);  // 韩文谚文
    }
}
