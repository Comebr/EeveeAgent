package com.azheng.boot.rag.core.recall;

import cn.hutool.core.collection.CollUtil;
import com.azheng.boot.rag.core.embedding.milvus.operation.MilvusOperations;
import com.azheng.boot.rag.core.embedding.milvus.operation.MilvusRetrievalResult;
import com.azheng.boot.rag.core.intent.IntentNode;
import com.azheng.boot.rag.core.intent.NodeScore;
import com.azheng.boot.rag.core.intent.SubQuestionIntent;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


/**
 * lc4j-recall-多路召回文本Chunk
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecallService {

    @Resource
    private MilvusOperations milvusOperations;

    @Qualifier("ragInnerRetrievalThreadPoolExecutor")
    private final Executor recallChunksExecutor;


    /**
     * 意图定向并行检索
     */
    public List<List<MilvusRetrievalResult>> recallMilvusContentWithKB(SubQuestionIntent subQuestionIntent, Integer TopK) {
        // 这里能进来的意图绝对是KB的

        // 粗筛：多路召回Chunk
        // 1.无意图，返回空
        if (CollUtil.isEmpty(subQuestionIntent.nodeScores())) {
            return List.of();
        }
        // 子问题
        String subQuestion = subQuestionIntent.subQuestion();

        // 子问题的KB意图
        List<NodeScore> nodeScores = subQuestionIntent.nodeScores();

        List<CompletableFuture<List<MilvusRetrievalResult>>> futures = nodeScores.stream()
                .map(
                        nodeScore -> {
                            CompletableFuture<List<MilvusRetrievalResult>> future = CompletableFuture.supplyAsync(
                                    () -> {
                                        // 1.获取节点信息
                                        IntentNode node = nodeScore.getNode();
                                        String collectionName = node.getCollectionName();

                                        // 2.检索出当前意图下的Chunk
                                        List<MilvusRetrievalResult> milvusRetrievalResults = milvusOperations.search(subQuestion, collectionName,TopK );
                                        return milvusRetrievalResults;
                                    }, recallChunksExecutor
                            );
                            return future;
                        }
                ).toList();

        List<List<MilvusRetrievalResult>> chunksList = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return CollUtil.isEmpty(chunksList) ? List.of() : chunksList;
    }

    /**
     * 向量全局并行检索
     * @param subQuestion
     * @param collections
     * @param TopK
     * @return
     */
    public List<List<MilvusRetrievalResult>> recallMilvusContentWithGlobal(String subQuestion,List<String> collections, Integer TopK) {
        if (CollUtil.isEmpty(collections)) {
            return List.of();
        }

        List<CompletableFuture<List<MilvusRetrievalResult>>> futures = collections.stream()
                .map(collection -> {
                    CompletableFuture<List<MilvusRetrievalResult>> future = CompletableFuture.supplyAsync(
                            () -> {
                                List<MilvusRetrievalResult> milvusRetrievalResults = milvusOperations.search(subQuestion, collection, TopK);
                                return milvusRetrievalResults;

                            }, recallChunksExecutor
                    );
                    return future;
                }).toList();

        List<List<MilvusRetrievalResult>> chunksList = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        return CollUtil.isEmpty(chunksList) ? List.of() : chunksList;
    }

}
