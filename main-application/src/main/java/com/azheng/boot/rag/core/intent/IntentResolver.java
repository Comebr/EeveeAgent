package com.azheng.boot.rag.core.intent;


import cn.hutool.core.collection.CollUtil;
import com.azheng.boot.rag.core.rewrite.RewriteResult;
import com.azheng.boot.rag.enums.IntentKind;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static com.azheng.boot.rag.constant.RAGConstant.INTENT_MIN_SCORE;
import static com.azheng.boot.rag.constant.RAGConstant.MAX_INTENT_COUNT;
import static com.azheng.boot.rag.enums.IntentKind.SYSTEM;


@Service
@RequiredArgsConstructor
public class IntentResolver {

    @Qualifier("defaultIntentClassifier")
    private final IntentClassifier intentClassifier;
    @Qualifier("intentClassifyThreadPoolExecutor")
    private final Executor intentClassifyExecutor;

    /**
     * 接收重写后的query，
     * 1.校验重写问题有效性
     * 2.对每个子问题进行意图分类
     * 3.合并意图分类结果
     * 4.返回意图分类结果
     * @param rewriteResult
     * @return
     */
    public List<SubQuestionIntent> resolve(RewriteResult rewriteResult) {
        List<String> subQuestions = new ArrayList<>();
        // 改动：子问题+精简问题 组合使用
        if(CollUtil.isNotEmpty(rewriteResult.subQuestions())){
            subQuestions.addAll(rewriteResult.subQuestions());
        }else {
            subQuestions.add(rewriteResult.rewrittenQuestion());
        }

        // 异步进行子问题意图识别
        List<CompletableFuture<SubQuestionIntent>> tasks = subQuestions.stream()
                .map(q -> CompletableFuture.supplyAsync(
                        () -> new SubQuestionIntent(q, classifyIntents(q)),
                        intentClassifyExecutor
                ))
                .toList();
        List<SubQuestionIntent> subIntents = tasks.stream()
                .map(CompletableFuture::join)
                .toList();

/*        //同步
        List<SubQuestionIntent> subIntents = subQuestions.stream()
                .map(q -> new SubQuestionIntent(q, classifyIntents(q)))
                .toList();*/
        //拦截：若未检测出至少一个意图：滚蛋


        return capTotalIntents(subIntents);
    }

    /**
     * 合并意图分类结果
     * 1.合并 MCP 意图
     * 2.合并 KB 意图
     * @param subIntents
     * @return
     */
    public IntentGroup mergeIntentGroup(List<SubQuestionIntent> subIntents) {
        List<NodeScore> mcpIntents = new ArrayList<>();
        List<NodeScore> kbIntents = new ArrayList<>();
        for (SubQuestionIntent si : subIntents) {
//            mcpIntents.addAll(filterMcpIntents(si.nodeScores()));
            kbIntents.addAll(filterKbIntents(si.nodeScores()));
        }
        return new IntentGroup(kbIntents);
    }

    /**
     * 判断意图是否仅包含系统意图
     * @param nodeScores
     * @return
     */
    public boolean isSystemOnly(List<NodeScore> nodeScores) {
        return nodeScores.size() == 1
                && nodeScores.get(0).getNode() != null
                && nodeScores.get(0).getNode().getKind() == SYSTEM;
    }

    /**
     * 对一个子问题进行意图分类
     * 1.调用意图分类器进行分类
     * 2.过滤出分数大于等于 INTENT_MIN_SCORE 的意图
     * 3.限制意图数量不超过 MAX_INTENT_COUNT
     * @param question
     * @param question
     * @return
     */
    private List<NodeScore> classifyIntents(String question) {
        List<NodeScore> scores = intentClassifier.classifyTargets(question);
        if (scores.isEmpty()) {
            return Collections.emptyList();
        }
        return scores.stream()
                .filter(ns -> ns.getScore() >= INTENT_MIN_SCORE)
                .limit(MAX_INTENT_COUNT)
                .toList();
    }

    /**
     * 过滤出 MCP 意图
     * 1.过滤出所有 MCP 意图
     * 2.过滤出所有 MCP 意图中包含工具 ID的意图
     * @param nodeScores
     * @return
     */
/*    private List<NodeScore> filterMcpIntents(List<NodeScore> nodeScores) {
        return nodeScores.stream()
                .filter(ns -> ns.getNode() != null && ns.getNode().getKind() == IntentKind.MCP)
                .filter(ns -> StrUtil.isNotBlank(ns.getNode().getMcpToolId()))
                .toList();
    }*/

    /**
     * 过滤出 KB 意图
     * 1.过滤出所有 KB 意图
     * 2.过滤出所有 KB 意图中包含工具 ID的意图
     * @param nodeScores
     * @return
     */
    private List<NodeScore> filterKbIntents(List<NodeScore> nodeScores) {
        return nodeScores.stream()
                .filter(ns -> {
                    IntentNode node = ns.getNode();
                    if (node == null) {
                        return false;
                    }
                    return node.getKind() == null || node.getKind() == IntentKind.KB;
                })
                .toList();
    }

    /**
     * 限制总意图数量不超过 MAX_INTENT_COUNT
     * <p>
     * 策略：
     * 1. 如果总数未超限，直接返回
     * 2. 如果超限，每个子问题至少保留 1 个最高分意图
     * 3. 剩余配额按分数从高到低分配给其他意图
     */
    private List<SubQuestionIntent> capTotalIntents(List<SubQuestionIntent> subIntents) {
        int totalIntents = subIntents.stream()
                .mapToInt(si -> si.nodeScores().size())
                .sum();

        // 未超限，直接返回
        if (totalIntents <= MAX_INTENT_COUNT) {
            return subIntents;
        }

        // 步骤1：收集所有意图，按子问题索引分组
        List<IntentCandidate> allCandidates = collectAllCandidates(subIntents);

        // 步骤2：每个子问题保留最高分意图
        List<IntentCandidate> guaranteedIntents = selectTopIntentPerSubQuestion(allCandidates, subIntents.size());

        // 步骤3：计算剩余配额
        int remaining = MAX_INTENT_COUNT - guaranteedIntents.size();

        // 步骤4：从剩余候选中按分数选择
        List<IntentCandidate> additionalIntents = selectAdditionalIntents(allCandidates, guaranteedIntents, remaining);

        // 步骤5：合并并重建结果
        return rebuildSubIntents(subIntents, guaranteedIntents, additionalIntents);
    }

    /**
     * 收集所有意图候选，标记所属子问题索引
     * 1.遍历所有子问题意图
     * 2.将每个意图转换为 IntentCandidate
     * 3.按分数降序排序
     * @param subIntents
     * @return
     */
    private List<IntentCandidate> collectAllCandidates(List<SubQuestionIntent> subIntents) {
        List<IntentCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < subIntents.size(); i++) {
            List<NodeScore> nodeScores = subIntents.get(i).nodeScores();
            if (CollUtil.isEmpty(nodeScores)) {
                continue;
            }
            for (NodeScore ns : nodeScores) {
                candidates.add(new IntentCandidate(i, ns));
            }
        }
        // 按分数降序排序
        candidates.sort((a, b) -> Double.compare(b.nodeScore().getScore(), a.nodeScore().getScore()));
        return candidates;
    }

    /**
     * 每个子问题选择最高分意图（保底策略）
     * 1.按分数降序排序
     * 2.保留每个子问题的最高分意图
     */
    private List<IntentCandidate> selectTopIntentPerSubQuestion(List<IntentCandidate> allCandidates, int subQuestionCount) {
        List<IntentCandidate> topIntents = new ArrayList<>();
        boolean[] selected = new boolean[subQuestionCount];
        //allCandidates长这样：[0号子问题(0.9分), 1号子问题(0.8分), 0号子问题(0.7分), 1号子问题(0.6分)]
        for (IntentCandidate candidate : allCandidates) {
            //获取当前队伍中的问题索引
            int index = candidate.subQuestionIndex();
            if (!selected[index]) {
                topIntents.add(candidate);
                //该问题索引拿到最高分之后，退出
                selected[index] = true;
            }
            // 所有子问题都有了保底意图，提前退出
            if (topIntents.size() == subQuestionCount) {
                break;
            }
        }
        return topIntents;
    }

    /**
     * 从剩余候选中选择额外意图
     * 1.按分数从高到低选择
     * 2.限制选择数量不超过 remaining
     */
    private List<IntentCandidate> selectAdditionalIntents(List<IntentCandidate> allCandidates,
                                                          List<IntentCandidate> guaranteedIntents,
                                                          int remaining) {
        if (remaining <= 0) {
            return List.of();
        }

        List<IntentCandidate> additional = new ArrayList<>();
        for (IntentCandidate candidate : allCandidates) {
            // 跳过已经被选为保底的意图
            if (guaranteedIntents.contains(candidate)) {
                continue;
            }
            additional.add(candidate);
            if (additional.size() >= remaining) {
                break;
            }
        }
        return additional;
    }

    /**
     * 根据选中的意图重建 SubQuestionIntent 列表
     * 1.合并所有选中的意图
     * 2.按子问题索引分组
     * 3.重建 SubQuestionIntent 列表
     * @param originalSubIntents 初筛的所有collection 问题 - 意图 问题A/0-List<node>、问题B/1-List<node>、问题C/2-List<node>
     * @param guaranteedIntents 最高分Collection 问题索引 - 意图 0-node  1-node
     * @param additionalIntents 补充的Collection 问题索引 - 意图 2-node
     * @return
     */
    private List<SubQuestionIntent> rebuildSubIntents(List<SubQuestionIntent> originalSubIntents,
                                                      List<IntentCandidate> guaranteedIntents,
                                                      List<IntentCandidate> additionalIntents) {
        // 合并所有选中的意图
        List<IntentCandidate> allSelected = new ArrayList<>(guaranteedIntents);
        if(CollUtil.isNotEmpty(additionalIntents)) {
            allSelected.addAll(additionalIntents);
        }

        // 按子问题索引分组
        Map<Integer, List<NodeScore>> groupedByIndex = new ConcurrentHashMap<>();
        for (IntentCandidate candidate : allSelected) {
            groupedByIndex.computeIfAbsent(candidate.subQuestionIndex(), k -> new ArrayList<>())
                    .add(candidate.nodeScore());
        }

        // 按顺序填充：问题索引对应的问题字符串+对应的Node
        // 问题A/0-List<node>、问题B/1-List<node>、问题C/2-List<node>
        //      0-node             1-node             2-node
        List<SubQuestionIntent> result = new ArrayList<>();
        for (int i = 0; i < originalSubIntents.size(); i++) {
            SubQuestionIntent original = originalSubIntents.get(i);
            List<NodeScore> retained = groupedByIndex.getOrDefault(i, List.of());
            result.add(new SubQuestionIntent(original.subQuestion(), retained));
        }
        return result;
    }
}
