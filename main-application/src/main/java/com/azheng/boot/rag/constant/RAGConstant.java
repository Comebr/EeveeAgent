package com.azheng.boot.rag.constant;

/**
 * RAG系统中的系统常量
 */
public class RAGConstant {

    public static final String USER_MESSAGE = "user";
    public static final String SYSTEM_MESSAGE = "system";
    public static final String AI_MESSAGE = "ai";


    public static final String KB_CONTEXT_HEADER = "## 文档内容";
    public static final String SYSTEM_CONTEXT_HEADER = "## 系统信息";

    public static final int DEFAULT_TOP_K = 10;

    /**
     * 单词查询拉取的意图数量上限，防止一次检索过的Collection导致性能问题
     */
    public static final int MAX_INTENT_COUNT = 3;
    /**
     * 意图识别最低分数
     */
    public static final double INTENT_MIN_SCORE = 0.35;

    public static final String MULTI_CHANNEL_KEY = "multi_channel";


    /// ————提示词模版路径———— ///
    /**
     * 意图识别提示词模版
     */
    public static final String INTENT_PROMPT_TEMPLATE_PATH = "prompt/Intent-recognition.st";

    /**
     * 系统对话提示词模板路径
     */
    public static final String CHAT_SYSTEM_PROMPT_TEMPLATE_PATH = "prompt/answer-chat-system.st";

    /**
     * 默认 RAG 问答提示词模板路径
     */
    public static final String RAG_KB_PROMPT_TEMPLATE_PATH = "prompt/answer-chat-kb.st";

    /**
     * 标题生成模版
     */
    public static final String TITLE_SUMMARY_TEMPLATE_PATH = "prompt/conversation-title-summary.st";

    /**
     * 多轮对话自动摘要模版
     */
    public static final String CONVERSATION_SUMMARY_TEMPLATE_PATH = "prompt/multiple-rounds-summary.st";

}
