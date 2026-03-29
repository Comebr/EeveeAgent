package com.azheng.agent.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    //流式聊天对话接口

    void streamingChat(String message, SseEmitter emitter);
}
