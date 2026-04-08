package com.azheng.boot.rag.chunking;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.*;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * lc4j-文本分块
 */
@Component
public class LangChain4jSplitter {

    /**
     * 可以实现一个策略工厂，根据用户选择的分块策略选用对应的分块器
     * 先做一个通用的递归分隔方案（默认）后续扩展多中分块策略
     */
    public List<TextSegment> splitterToChunk(Document document, ChunkOptions chunkOptions) {
        //复合分割器(推荐)
        HierarchicalDocumentSplitter splitter = (HierarchicalDocumentSplitter) DocumentSplitters
                                                .recursive(chunkOptions.getMaxChunkSize(),chunkOptions.getMaxOverlapSize());
        return splitter.split(document);
    }

}
