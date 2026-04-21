package com.azheng.boot.rag.service;

import com.azheng.boot.rag.controller.request.DeleteMessageRequest;
import com.azheng.boot.rag.controller.vo.ConversationMessageVO;

import java.util.List;

public interface ConversationMessageService {

    /**
     * 获取当前会话消息列表（引用）
     */
    List<ConversationMessageVO> queryConversationMessage(String conversationId);

    /**
     * 在当前会话新增消息（追加聊天，具体策略待定）
     */
    void saveConversationMessage(String conversationId, String userId, String role, String content);

    /**
     * 删除指定消息，便于手动管理会话上下文（同时删除缓存）
     */
    void deleteByMessageId(DeleteMessageRequest request);


}
