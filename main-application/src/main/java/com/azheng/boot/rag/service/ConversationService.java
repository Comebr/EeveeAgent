package com.azheng.boot.rag.service;

import com.azheng.boot.rag.controller.vo.ConversationMessageVO;
import com.azheng.boot.rag.controller.vo.ConversationVO;

import java.util.List;

public interface ConversationService {

    /**
     * 根据当前用户id查询其会话历史列表
     */
    List<ConversationVO> queryConversations(String userId);

    /**
     * 修改会话标题
     */
    void mkdirTitle(String newTitle, String conversationId);

    /**
     * 删除指定会话
     */
    void deleteByConversationId(String conversationId);

    /**
     * 获取会话消息列表（引用）
     */
    List<ConversationMessageVO> queryConversationMessage(String conversationId);

    /**
     * 为本次会话生成标题
     * （策略待定，可以是首次对话的摘要总结，也可以是总对话总结，但是长度限制在12个字符以内）
     */
    void generateTitle(String conversationId, String firstQuestion);
}
