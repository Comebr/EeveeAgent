package com.azheng.boot.rag.core.embedding.milvus.operation;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class MilvusRetrievalResult {

    /**
     * 主键ID
     */
    private Long chunkId;
    /**
     * 文本内容
     */
    private String text;
    /**
     * 元数据（Map 格式，无 JsonNull 报错）
     */
    private Map<String, Object> metadata;
    /**
     * 相似度分数
     */
    private Float score;
}
