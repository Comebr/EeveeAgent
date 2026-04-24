package com.azheng.boot.rag.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RAGChatService {
    //流式聊天对话接口 生成
    void streamingChat(String prompt, String roundId, String conversationId, Boolean openThinking, SseEmitter emitter);

    void cancel(String roundId);
}
