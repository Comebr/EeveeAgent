package com.azheng.boot.rag.core.retrieval.chanel.strategy;

import cn.hutool.core.collection.CollUtil;
import com.azheng.boot.rag.config.SearchChannelProperties;
import com.azheng.boot.rag.core.embedding.milvus.operation.MilvusRetrievalResult;
import com.azheng.boot.rag.core.intent.NodeScore;
import com.azheng.boot.rag.core.intent.SubQuestionIntent;
import com.azheng.boot.rag.core.recall.RecallService;
import com.azheng.boot.rag.core.rerank.ReRankContext;
import com.azheng.boot.rag.core.retrieval.chanel.SearchChannel;
import com.azheng.boot.rag.core.retrieval.chanel.SearchChannelResult;
import com.azheng.boot.rag.core.retrieval.chanel.SearchChannelType;
import com.azheng.boot.rag.core.retrieval.chanel.SearchContext;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 意图定向检索
 * <p>
 * 根据意图检索意图所指向的知识库
 */
@Slf4j
@Component
public class IntentDirectedSearchChannel implements SearchChannel {
    //系统检索配置
    private final SearchChannelProperties properties;

    @Resource
    private RecallService recallService;

    public IntentDirectedSearchChannel(SearchChannelProperties properties) {

        this.properties = properties;
    }

    @Override
    public String getName() {
        return "IntentDirectedSearchChannel";
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean isEnabled(SearchContext context) {
        if(!properties.getChannels().getIntentDirected().isEnabled()){
            return false;
        }
        // 无意图不开启
        if(CollUtil.isEmpty(context.getIntents())){
            return false;
        }

        //无KB意图不开启
        List<NodeScore> nodeScores = fileterKbIntents(context);
        return CollUtil.isNotEmpty(nodeScores);
    }


    @Override
    public SearchChannelResult search(SearchContext context) {
        long startTime = System.currentTimeMillis();
        try {
            //1.从检索上下文中获取
            List<NodeScore> kbIntents = fileterKbIntents(context);

            if (CollUtil.isEmpty(kbIntents)) {
                log.warn("意图定向检索通道被启用，但未找到 KB 意图（不应该发生）");
                return SearchChannelResult.builder()
                        .channelType(SearchChannelType.INTENT_DIRECTED)
                        .channelName(getName())
                        .chunks(List.of())
                        .confidence(0.0)
                        .latencyMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            log.info("执行意图定向检索，识别出 {} 个 KB 意图", kbIntents.size());

            // 并行检索所有意图对应的知识库
            int topKMultiplier = properties.getChannels().getIntentDirected().getTopKMultiplier();
            SubQuestionIntent subQuestionIntent = new SubQuestionIntent(context.getMainQuestion(), kbIntents);
            List<List<MilvusRetrievalResult>> lists = recallService.recallMilvusContentWithKB(subQuestionIntent,topKMultiplier );
            List<MilvusRetrievalResult> recalls = lists.stream()
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .toList();

            List<ReRankContext> allChunks = convertToReRankContext(recalls);


            // 计算置信度（基于意图分数）
            double confidence = kbIntents.stream()
                    .mapToDouble(NodeScore::getScore)
                    .max()
                    .orElse(0.0);

            long latency = System.currentTimeMillis() - startTime;

            log.info("意图定向检索完成，检索到 {} 个 Chunk，置信度：{}，耗时 {}ms",
                    allChunks.size(), confidence, latency);

            return SearchChannelResult.builder()
                    .channelType(SearchChannelType.INTENT_DIRECTED)
                    .channelName(getName())
                    .chunks(allChunks)
                    .confidence(confidence)
                    .latencyMs(latency)
                    .metadata(Map.of("intentCount", kbIntents.size()))
                    .build();
        } catch (Exception e) {
            log.error("意图定向检索失败", e);
            return SearchChannelResult.builder()
                    .channelType(SearchChannelType.INTENT_DIRECTED)
                    .channelName(getName())
                    .chunks(List.of())
                    .confidence(0.0)
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 提取 KB 意图
     */
    private List<NodeScore> fileterKbIntents(SearchContext context) {
        double minScore = properties.getChannels().getIntentDirected().getMinIntentScore();
        return context.getIntents().stream()
                .flatMap(si -> si.nodeScores().stream())
                .filter(ns -> ns.getNode() != null && ns.getNode().isKB())
                .filter(ns -> ns.getScore() >= minScore)
                .toList();
    }

    /**
     * Milvus检索结果 转换为 重排序上下文
     * @param milvusResults 原始检索结果列表
     * @return 重排序上下文列表
     */
    public static List<ReRankContext> convertToReRankContext(List<MilvusRetrievalResult> milvusResults) {
        if (CollectionUtils.isEmpty(milvusResults)) {
            return Collections.emptyList();
        }

        // 流式转换，字段一一映射
        return milvusResults.stream().map(result -> {
            ReRankContext context = new ReRankContext();
            context.setId(result.getChunkId() != null ? result.getChunkId().toString() : null);
            context.setText(result.getText());
            context.setRelevanceScore(result.getScore());
            return context;
        }).collect(Collectors.toList());
    }



    @Override
    public SearchChannelType getType() {
        return SearchChannelType.INTENT_DIRECTED;
    }
}
