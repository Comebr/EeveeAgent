package com.azheng.boot.rag.core.retrieval.chanel;

import com.azheng.boot.rag.core.intent.SubQuestionIntent;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SearchContext {
    /**
     * 原始子问题问题
     */
    private String originalQuestion;

    /**
     * 重写后的子问题
     */
    private String rewrittenQuestion;

    /**
     * 子问题列表
     */
    private List<String> subQuestions;

    /**
     * 意图识别结果
     */
    private List<SubQuestionIntent> intents;

    /**
     * 期望返回的结果数量
     */
    private int topK;

    /**
     * 扩展元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 获取主问题（优先使用重写后的问题），还是子问题
     */
    public String getMainQuestion() {
        return rewrittenQuestion != null ? rewrittenQuestion : originalQuestion;
    }
}