package com.azheng.boot.kb.controller.request;

import com.azheng.boot.rag.chunking.ChunkOptions;
import lombok.Data;

/**
 * 启动分块请求体
 */
@Data
public class StartChunkRequest {

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 分块参数配置（有默认值）
     */
    private ChunkOptions chunkOptions;
}
