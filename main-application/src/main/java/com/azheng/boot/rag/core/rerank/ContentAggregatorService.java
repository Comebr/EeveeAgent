package com.azheng.boot.rag.core.rerank;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.query.Query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * lc4j-rerank-聚合重排
 * 处理
 */
public class ContentAggregatorService {


    /**
     * 基于 RRF（互惠排名融合）算法实现多源结果融合（默认实现）
     * @param rerankRequestMap
     * @return
     */
    public List<Content> contentAggregatorWithRRF(Map<Query, Collection<List<Content>>> rerankRequestMap){
        DefaultContentAggregator defaultContentAggregator = new DefaultContentAggregator();
        List<Content> aggregate = defaultContentAggregator.aggregate(rerankRequestMap);
        return aggregate;
    }


    /**
     * 高精度语义重排
     * @param rerankRequestMap
     * @return
     */
    public List<Content> contentAggregatorWithReRank(Map<Query, Collection<List<Content>>> rerankRequestMap){
        /**
         * 参数列表：
         * .scoringModel()    // 1. 重排序打分模型（核心）
         * .minScore()        // 2. 最小分数阈值（过滤）
         * .maxResults()      // 3. 最大返回结果数（截断）TopN
         * .querySelector()   // 4. 查询提取器（定位用户问题）
         */
        ReRankingContentAggregator reRankingContentAggregator = ReRankingContentAggregator
                .builder()
                .minScore(0.35)
                .maxResults(10) //TopN
                .build();
        List<Content> aggregate = reRankingContentAggregator.aggregate(rerankRequestMap);
        return aggregate;
    }
}
