package com.azheng.boot.rag.core.prompt;

public enum PromptScene {
    /**
     * 知识库提示词模版
     */
    KB_ONLY,

    /**
     * 系统提示词模版
     */
    SYSTEM_ONLY,

    /**
     * 混合（all）
     */
    MIXED,

    /**
     * 不参考任何提示词
     */
    EMPTY
}