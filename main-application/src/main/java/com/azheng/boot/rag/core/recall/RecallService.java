package com.azheng.boot.rag.core.recall;

import cn.hutool.core.collection.CollUtil;
import com.azheng.boot.rag.core.embedding.milvus.config.MilvusEmbeddingStoreConfig;
import com.azheng.boot.rag.core.embedding.milvus.operation.MilvusOperations;
import com.azheng.boot.rag.core.embedding.milvus.operation.MilvusRetrievalResult;
import com.azheng.boot.rag.core.intent.IntentNode;
import com.azheng.boot.rag.core.intent.NodeScore;
import com.azheng.boot.rag.core.intent.SubQuestionIntent;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.*;

import static com.azheng.boot.rag.constant.RAGConstant.INTENT_MIN_SCORE;

/**
 * lc4j-recall-多路召回文本Chunk
 */
@Slf4j
@Service
public class RecallService {

    @Resource
    private MilvusEmbeddingStoreConfig milvusEmbeddingStoreConfig;

    @Resource
    private MilvusOperations milvusOperations;

    @Resource
    private EmbeddingModel embeddingModel;

    @Value("${recall.top-n}")
    private Integer TopN;
    /**
     * retrieval-for-lc4j-ContentAggregator
     * 对一个子问题进行召回
     *
     * @return
     */
    public Map<String, List<MilvusRetrievalResult>> recallMilvusContentWithKB(SubQuestionIntent subQuestionIntent) {
        // 这里能进来的意图绝对是KB的
        // 当前子问题召回的Chunks总列表
        Map<String,List<MilvusRetrievalResult>> reCallContextMap = new HashMap<>();

        // 粗筛：多路召回Chunk
            // 1.无意图，返回空
            if(CollUtil.isEmpty(subQuestionIntent.nodeScores())){
                return Map.of();
            }
            // 子问题
            String subQuestion = subQuestionIntent.subQuestion();

            // 子问题的KB意图
            List<NodeScore> nodeScores = subQuestionIntent.nodeScores();

            // 召回所有意图（一个子问题可能存在多个KB意图）
            nodeScores.stream(). forEach(nodeScore -> {

                // 1.获取节点信息
                IntentNode node = nodeScore.getNode();
                String collectionName = node.getCollectionName();
                String nodeId = node.getId();

                // 2.检索出当前意图下的Chunk
                List<MilvusRetrievalResult> milvusRetrievalResults = milvusOperations.search(subQuestion, collectionName);

                // 3.追加本次node的召回结果
                reCallContextMap.put(nodeId, milvusRetrievalResults);
            });
        // 4.返回检索总结果
        return reCallContextMap;
    }

    public Collection<List<Content>> recallMilvusContentInLc4j(String question,List<SubQuestionIntent> subQuestionIntentList) {
        //总Chunks列表
        Collection<List<Content>> contentsList = new ArrayList<>();

        // 粗筛：多路召回Chunk
        for (SubQuestionIntent subQuestionIntent : subQuestionIntentList) {
            // 1.无意图结果，下一个
            if(CollUtil.isEmpty(subQuestionIntent.nodeScores())){
                continue;
            }
            String subQuestion = subQuestionIntent.subQuestion();
            List<NodeScore> nodeScores = subQuestionIntent.nodeScores();
            nodeScores.stream(). forEach(nodeScore -> {
                IntentNode node = nodeScore.getNode();
                String collectionName = node.getCollectionName();
                String nodeId = node.getId();
                // 2.锁定集合Collection
                MilvusEmbeddingStore milvusEmbeddingStore = milvusEmbeddingStoreConfig.creteMilvusEmbeddingStore(collectionName);

                // 3.构建向量检索器
                EmbeddingStoreContentRetriever embeddingStoreContentRetriever = EmbeddingStoreContentRetriever
                        .builder()
                        .minScore(INTENT_MIN_SCORE) //最低分数阈值，会自动过滤
                        .maxResults(TopN) //TopN 单次最大检索Content数量
                        .embeddingModel(embeddingModel) //嵌入模型
                        .embeddingStore(milvusEmbeddingStore) //嵌入存储
                        .build();
                /**
                 * !!! Content中的metadata还包含对Chunk的评分score以及id !!!
                 * 这是做rerank的关键！
                 */
                // 4.检索出当前意图下的Chunk

                Query query = Query.from(subQuestion);
                List<Content> contents = embeddingStoreContentRetriever.retrieve(query);

                //5.追加本次进总召回列表
                contentsList.add(contents);
            });
        }
        // 6.返回检索总结果
        return contentsList;
    }
}
