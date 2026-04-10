package com.azheng.boot.kb.controller.request;

import lombok.Data;

@Data
public class InsertChunkRequest {
    /**
     * 在哪个知识库下新增的知识块？
     */
    private String kbId;

    /**
     * 在哪个文档下新增的知识块？
     */
    private String docId;

    /**
     * 知识库的内容
     */
    private String chunkText;
}
