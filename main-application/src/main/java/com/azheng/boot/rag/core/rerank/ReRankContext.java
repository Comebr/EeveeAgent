package com.azheng.boot.rag.core.rerank;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReRankContext {

    /**
     * 命中记录的唯一标识
     * 比如向量库中的 primary key 或文档 id
     */
    private String id;


    /**
     * 命中得分
     * 数值越大表示与查询的相关性越高
     */
    private Float relevanceScore;

    /**
     * 命中的文本内容
     * 一般是被切分后的文档片段或段落
     */
    private String text;
}
