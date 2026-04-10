package com.azheng.boot.kb.service;

import com.azheng.boot.kb.controller.request.InsertChunkRequest;
import com.azheng.boot.kb.controller.request.MkdirChunkTextRequest;
import com.azheng.boot.kb.controller.request.PageQueryChunkRequest;
import com.azheng.boot.kb.controller.request.StartChunkRequest;
import com.azheng.boot.kb.controller.vo.KnowledgeChunkVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface KnowledgeChunkService {

    /**
     * 执行分块
     */
    void performChunking(StartChunkRequest startChunkRequest);

    /**
     *  分页查询知识块
     */
    IPage<KnowledgeChunkVO> pageQuery(PageQueryChunkRequest pageQueryChunkRequest);

    /**
     * 修改知识块内容
     */
    void updateChunkText(MkdirChunkTextRequest request);

    /**
     * 删除知识块
     * @param chunkId
     */
    void remove(String chunkId);

    /**
     * 新增单条知识块
     */
    void insertChunk(InsertChunkRequest request);
}
