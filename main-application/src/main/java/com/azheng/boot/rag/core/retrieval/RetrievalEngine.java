package com.azheng.boot.rag.core.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.rag.core.embedding.milvus.operation.MilvusRetrievalResult;
import com.azheng.boot.rag.core.intent.IntentNode;
import com.azheng.boot.rag.core.intent.NodeScore;
import com.azheng.boot.rag.core.intent.SubQuestionIntent;
import com.azheng.boot.rag.core.prompt.ContextFormatter;
import com.azheng.boot.rag.core.recall.RecallService;
import com.azheng.boot.rag.core.rerank.DashScopeRerankService;
import com.azheng.boot.rag.core.rerank.ReRankContext;
import com.azheng.boot.rag.enums.IntentKind;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static com.azheng.boot.rag.constant.RAGConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalEngine {

    @Resource
    private RecallService recallService;

    @Resource
    private DashScopeRerankService dashScopeRerankService;

    @Qualifier("ragContextThreadPoolExecutor")
    private  Executor ragContextExecutor;

    @Resource
    private ContextFormatter contextFormatter;



    public RetrievalContext retrieval(List<SubQuestionIntent> subIntents, int topK){
        if (CollUtil.isEmpty(subIntents)) {
            return RetrievalContext.builder()
                    .kbContext("")
                    .recallChunks(Map.of())
                    .build();
        }

        // 1，根据子问题意图列表，计算每个子问题的实际 TopK 数
        // 2，为每个子问题执行多通道检索（知识库）
        int finalTopK = topK > 0 ? topK : DEFAULT_TOP_K;

/*        List<CompletableFuture<SubQuestionContext>> tasks = subIntents.stream()
                .map(si -> CompletableFuture.supplyAsync(
                        () -> buildSubQuestionContext(
                                si,
                                resolveSubQuestionTopK(si, finalTopK)
                        ),
                        ragContextExecutor
                ))
                .toList();
        List<SubQuestionContext> contexts = tasks.stream()
                .map(CompletableFuture::join)
                .toList();*/
        List<SubQuestionContext> contexts = subIntents.stream()
                .map(si -> buildSubQuestionContext(si, resolveSubQuestionTopK(si, finalTopK)))
                .toList();

        // 3，合并所有子问题的检索结果，生成最终的检索上下文
        StringBuilder kbBuilder = new StringBuilder();
        Map<String, List<ReRankContext>> mergedIntentChunks = new ConcurrentHashMap<>();

        for (SubQuestionContext context : contexts) {
            if (StrUtil.isNotBlank(context.kbContext())) {
                appendSection(kbBuilder, context.question(), context.kbContext());
            }
            if (CollUtil.isNotEmpty(context.intentChunks())) {
                mergedIntentChunks.putAll(context.intentChunks());
            }
        }

        return RetrievalContext.builder()
                .kbContext(kbBuilder.toString().trim())
                .recallChunks(mergedIntentChunks)
                .build();
    }

    private SubQuestionContext buildSubQuestionContext(SubQuestionIntent intent, int topK) {
        List<NodeScore> kbIntents = filterKbIntents(intent.nodeScores());

        KbResult kbResult = retrieveAndRerank(intent, kbIntents, topK);

        return new SubQuestionContext(intent.subQuestion(), kbResult.groupedContext(), kbResult.intentChunks());
    }
    private KbResult retrieveAndRerank(SubQuestionIntent intent, List<NodeScore> kbIntents, int topK) {

        /**
         * recall召回
         */
        //召回结果
        Map<String, List<MilvusRetrievalResult>> subQuestionRecallMap = recallService.recallMilvusContentWithKB(intent);

        // 收集文档
        List<String> documents = new ArrayList<>();
        subQuestionRecallMap.forEach((k, v) -> {
            List<String> list = v.stream().map(MilvusRetrievalResult::getText).toList();
            documents.addAll(list);
        });

        // 检查文档列表是否为空
        if (CollUtil.isEmpty(documents)) {
            return KbResult.empty();
        }

        /**
         * Rerank 重排
         */
        List<ReRankContext> suQuestionReRankList = dashScopeRerankService.rerank(
                                                                                                    intent.subQuestion(),
                                                                                                    documents);

        if(CollUtil.isEmpty(suQuestionReRankList)){
            return KbResult.empty();
        }
        // 可以设定一个最低分阈值进行过滤


        // 按意图节点分组（用于格式化上下文）
        Map<String, List<ReRankContext>> intentChunks = new ConcurrentHashMap<>();
        // 如果有意图识别结果，按意图节点 ID 分组
        if (CollUtil.isNotEmpty(kbIntents)) {
            // 将所有 chunks 按意图节点 ID 分配
            // 注意：多通道检索返回的 chunks 无法精确对应到某个意图节点
            // 所以我们将所有 chunks 分配给每个意图节点
            for (NodeScore ns : kbIntents) {
                intentChunks.put(ns.getNode().getId(), suQuestionReRankList);
            }
        } else {
            // 如果没有意图识别结果，使用特殊 key
            intentChunks.put(MULTI_CHANNEL_KEY, suQuestionReRankList);
        }

        /**
         * 聚合成一个文本字符串groupContext
         */

        String groupedContext = contextFormatter.formatKbContext(kbIntents, intentChunks, topK);
        /**
         * 合并为KbResult:groupContext、Map
         */

        return new KbResult(groupedContext, intentChunks);
    }



    private int resolveSubQuestionTopK(SubQuestionIntent intent, int fallbackTopK) {
        return filterKbIntents(intent.nodeScores()).stream()
                .map(NodeScore::getNode)
                .filter(Objects::nonNull)
                .map(IntentNode::getTopK)
                .filter(Objects::nonNull)
                .filter(topK -> topK > 0)
                .max(Integer::compareTo)
                .orElse(fallbackTopK);
    }


    /**
     * 过滤出KB检索意图
     * @param nodeScores
     * @return
     */
    private List<NodeScore> filterKbIntents(List<NodeScore> nodeScores) {
        return nodeScores.stream()
                .filter(ns -> ns.getScore() >= INTENT_MIN_SCORE)
                .filter(ns -> {
                    IntentNode node = ns.getNode();
                    if (node == null) {
                        return false;
                    }
                    return node.getKind() == null || node.getKind() == IntentKind.KB;
                })
                .toList();
    }

    private void appendSection(StringBuilder builder, String question, String context) {
        builder.append("---\n")
                .append("**子问题**：").append(question).append("\n\n")
                .append("**相关文档**：\n")
                .append(context).append("\n\n");
    }

    private record SubQuestionContext(String question,
                                      String kbContext,
                                      Map<String, List<ReRankContext>> intentChunks) {
    }
}
