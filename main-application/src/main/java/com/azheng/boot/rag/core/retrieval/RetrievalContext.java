package com.azheng.boot.rag.core.retrieval;

import cn.hutool.core.util.StrUtil;
import com.azheng.boot.rag.core.rerank.ReRankContext;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 最终检索结果
 */
@Data
@Builder
public class RetrievalContext {

    /**
     * 召回重排的结果
     */
    private String kbContext;

    /**
     * 意图 ID -> 分片列表
     */
    private Map<String, List<ReRankContext>> recallChunks;


    /**
     * 是否存在 KB 上下文
     */
    public boolean hasKb() {
        return StrUtil.isNotBlank(kbContext);
    }

    /**
     * 是否无任何上下文
     */
    public boolean isEmpty() {
        return !hasKb();
    }
}
