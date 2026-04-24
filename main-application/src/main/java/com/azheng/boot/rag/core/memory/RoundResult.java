package com.azheng.boot.rag.core.memory;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 轮次判断结果
 *
 * @param isBeyond    是否超出
 * @param rounds      轮数
 * @param validRounds 过滤掉单条消息的纯轮次列表
 */
public record RoundResult(boolean isBeyond, int rounds, List<ChatMessage> validRounds){
}
