package com.azheng.boot.rag.controller.request;

import lombok.Data;

@Data
public class RAGChatRequest {

    /**
     * 当前会话的用户id标识
     */
    private String userId;

    /**
     * 用户提问信息
     */
    private String message;
}
