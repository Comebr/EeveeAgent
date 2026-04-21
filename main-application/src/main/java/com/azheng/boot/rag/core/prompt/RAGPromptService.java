package com.azheng.boot.rag.core.prompt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.rag.core.intent.IntentNode;
import com.azheng.boot.rag.core.intent.NodeScore;
import com.azheng.boot.rag.core.rerank.ReRankContext;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import dev.langchain4j.data.message.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.azheng.boot.rag.constant.RAGConstant.*;

@Service
@RequiredArgsConstructor
public class RAGPromptService {

    @Resource
    private PromptTemplateLoader promptTemplateLoader;

    /**
     * 生成系统提示词，并对模板格式做清理
     */
    public String buildSystemPrompt(PromptContext context) {
        PromptBuildPlan plan = plan(context);
        String template = StrUtil.isNotBlank(plan.getBaseTemplate())
                ? plan.getBaseTemplate()
                : switchTemplate(plan.getScene());
        return StrUtil.isBlank(template) ? "" : PromptTemplateUtils.cleanupPrompt(template);
    }

    private PromptBuildPlan plan(PromptContext context) {
        if (context.hasKb()) {
            return planKbOnly(context);
        }
        throw new IllegalStateException("PromptContext requires MCP or KB context.");
    }

    private PromptBuildPlan planKbOnly(PromptContext context) {
        PromptPlan plan = planPrompt(context.getKbIntents(), context.getIntentChunks());
        return PromptBuildPlan.builder()
                .scene(PromptScene.KB_ONLY)
                .baseTemplate(plan.getBaseTemplate())
                .kbContext(context.getKbContext())
                .question(context.getQuestion())
                .build();
    }

    private PromptPlan planPrompt(List<NodeScore> intents, Map<String, List<ReRankContext>> intentChunks) {
        List<NodeScore> safeIntents = intents == null ? Collections.emptyList() : intents;

        // 1) 先剔除“未命中检索”的意图
        List<NodeScore> retained = safeIntents.stream()
                .filter(ns -> {
                    IntentNode node = ns.getNode();
                    String key = nodeKey(node);
                    List<ReRankContext> chunks = intentChunks == null ? null : intentChunks.get(key);
                    return CollUtil.isNotEmpty(chunks);
                })
                .toList();

        if (retained.isEmpty()) {
            // 没有任何可用意图：无基模板（上层可根据业务选择 fallback）
            return new PromptPlan(Collections.emptyList(), null);
        }

        // 2) 单 / 多意图的模板与片段策略
        if (retained.size() == 1) {
            IntentNode only = retained.get(0).getNode();
            String tpl = StrUtil.emptyIfNull(only.getPromptTemplate()).trim();

            if (StrUtil.isNotBlank(tpl)) {
                // 单意图 + 有模板：使用模板本身
                return new PromptPlan(retained, tpl);
            } else {
                // 单意图 + 无模板：走默认模板
                return new PromptPlan(retained, null);
            }
        } else {
            // 多意图：统一默认模板
            return new PromptPlan(retained, null);
        }
    }

    /**
     * 构建最终提示词
     * @param context 提示词上下文
     * @param history 会话记忆
     * @param question 精简版原问题
     * @param subQuestions 扩展子问题
     * @return
     */
    public List<ChatMessage> buildStructuredMessages(PromptContext context,
                                                     List<ChatMessage> history,
                                                     String question,
                                                     List<String> subQuestions){
        // 1.构造聊天消息集合
        List<ChatMessage> messages = new ArrayList<>();

        /**
         * 2.构建系统提示词（前置必备）
         * 内部：判断筛选问答场景 ——> 根据场景构建具体的提示词模版
         */
        String systemPrompt = buildSystemPrompt(context);

        /**
         * 3.添加系统消息
         * systemPrompt：告诉AI当前场景下"你是谁、你能做什么、怎么回答"
         * 这一步是Agent的核心，将大众化的AI模型，转化为专业角色
         */
        if(StrUtil.isNotBlank(systemPrompt)){
            messages.add(new SystemMessage(systemPrompt));
        }

        /**
         * 4.添加知识库内容
         * kbContext：给AI提供的依据,检索出的TopN，Chunk，用于辅助AI回答用户
         */
        if(StrUtil.isNotBlank(context.getKbContext())){
            messages.add(new UserMessage(formatEvidence( KB_CONTEXT_HEADER,context.getKbContext() )));
        }


        // 6.将用户提问转为UserMessage
        if(StrUtil.isNotBlank(question)){
            messages.add(new UserMessage(question));
        }

        return messages;
    }

    /**
     * 筛选场景
     * @param promptScene 场景
     * @return String 提示词模版
     */
    private String switchTemplate(PromptScene promptScene) {
        return switch (promptScene) {
            case KB_ONLY -> promptTemplateLoader.load(RAG_KB_PROMPT_TEMPLATE_PATH);
            case SYSTEM_ONLY -> promptTemplateLoader.load(CHAT_SYSTEM_PROMPT_TEMPLATE_PATH);
            case MIXED -> promptTemplateLoader.load(RAG_KB_PROMPT_TEMPLATE_PATH);
            case EMPTY -> "";
        };
    }

    /**
     *
     * @param context 提示词上下文
     * @return PromptScene 场景
     */
    private static PromptScene choseScene(PromptContext context) {
        if(context.hasKb()){
            return PromptScene.KB_ONLY;
        }
        return PromptScene.EMPTY;
    }

    private String formatEvidence(String header, String body) {
        return header + "\n" + body.trim();
    }

    /**
     * 从意图节点提取用于映射检索结果的 key
     */
    private static String nodeKey(IntentNode node) {
        if (node == null) return "";
        if (StrUtil.isNotBlank(node.getId())) return node.getId();
        return String.valueOf(node.getId());
    }
}
