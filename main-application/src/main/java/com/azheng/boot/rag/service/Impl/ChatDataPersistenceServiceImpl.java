package com.azheng.boot.rag.service.Impl;

import com.azheng.boot.rag.dao.entity.ConversationDO;
import com.azheng.boot.rag.dao.mapper.ConversationMapper;
import com.azheng.boot.rag.service.ChatDataPersistenceService;
import com.azheng.boot.rag.service.ConversationMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static com.azheng.boot.rag.constant.RAGConstant.AI_MESSAGE;
import static com.azheng.boot.rag.constant.RAGConstant.USER_MESSAGE;

@Service
@RequiredArgsConstructor
public class ChatDataPersistenceServiceImpl implements ChatDataPersistenceService {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageService conversationMessageService;

    /**
     * 聊天消息持久化
     * @param actualConversationId
     * @param userId
     * @param userQuestion
     * @param responseText
     * @param isFirstChat
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void mysqlPersistent(String actualConversationId, String userId, String userQuestion, String responseText, Boolean isFirstChat) {
        // 1.创建会话记录-MySQL
        if(isFirstChat){
            ConversationDO conversation = ConversationDO.builder()
                    .conversationId(Long.parseLong(actualConversationId))
                    .userId(Long.parseLong(userId))
                    .title("新对话")
                    .lastTalkTime(new Date())
                    .build();
            conversationMapper.insert(conversation);
        }

        // 2.保存用户消息至会话消息表-MySQL
        conversationMessageService.saveConversationMessage(actualConversationId, userId,USER_MESSAGE, userQuestion);


        // 3.保存AI回复至MySQL
        conversationMessageService.saveConversationMessage(actualConversationId, userId,AI_MESSAGE, responseText);
    }
}
