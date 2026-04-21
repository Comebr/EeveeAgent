package com.azheng.boot.rag.service;

public interface ChatDataPersistenceService {

    void mysqlPersistent(String actualConversationId,
                         String userId,
                         String userQuestion,
                         String responseText,
                         Boolean isFirstChat);
}
