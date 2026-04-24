package com.azheng.boot.rag.core.rerank;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankOutput;
import com.alibaba.dashscope.rerank.TextReRankParam;
import com.alibaba.dashscope.rerank.TextReRankResult;
import com.azheng.framework.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * DashScope-rerank
 */
@Slf4j
@Service
public class DashScopeRerankService {

    @Value("${langchain4j.community.dashscope.api-key}")
    private String apiKey;


    /**
     * 重排用的Query是原问题,召回得到的结果只用传过来文本，不用传召回文本的得分，因为重排不会参考
     * 那Rerank也是原子性，只对一个子问题
     * 获得最终精排后的Contents
     */
    public List<ReRankContext> rerank(String suQuestion, List<ReRankContext> chunks, Integer topK) {
        TextReRank textReRank = new TextReRank();

        List<ReRankContext> reRankContexts = new ArrayList<>();
        List<String> documents = chunks.stream().map(ReRankContext::getText).toList();

        TextReRankParam textReRankParam = TextReRankParam
                                                .builder()
                                                .query(suQuestion)
                                                .documents(documents)
                                                .topN(topK)
                                                .apiKey(apiKey)
                                                .model(TextReRank.Models.GTE_RERANK_V2)
                                                .returnDocuments(true)
                                                .build();

        try {
            TextReRankResult result = textReRank.call(textReRankParam);

            List<TextReRankOutput.Result> results = result.getOutput().getResults();
            //封装结果：score、Text、
            for (TextReRankOutput.Result result1 : results) {
                float relevanceScore = result1.getRelevanceScore().floatValue();
                String text = result1.getDocument().getText();
                ReRankContext rankContext = ReRankContext.builder()
                        .id(result1.getIndex().toString())
                        .relevanceScore(relevanceScore)
                        .text(text)
                        .build();
                reRankContexts.add(rankContext);
            }
            return reRankContexts;
        } catch (NoApiKeyException e) {
            throw new ServiceException("未配置API Key，请设置环境变量 DASHSCOPE_API_KEY");
        } catch (InputRequiredException e) {
            throw new ServiceException("参数校验失败:" + e.getMessage());
        } catch (ApiException e) {
            throw new ServiceException("API调用异常: " + e.getMessage());
        }
    }
}
