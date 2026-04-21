package com.azheng.boot.rag.service.Impl;

import cn.hutool.core.lang.Assert;
import com.azheng.boot.rag.controller.request.DeleteMessageRequest;
import com.azheng.boot.rag.controller.vo.ConversationMessageVO;
import com.azheng.boot.rag.dao.entity.ConversationMessageDO;
import com.azheng.boot.rag.dao.mapper.ConversationMessageMapper;
import com.azheng.boot.rag.service.ConversationMessageService;
import com.azheng.framework.exception.ClientException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationMessageServiceImpl implements ConversationMessageService {

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource(name = "qwen-turbo")
    private ChatModel chatModel;

    @Override
    public List<ConversationMessageVO> queryConversationMessage(String conversationId) {
        List<ConversationMessageDO> conversationMessageDOList = conversationMessageMapper.selectList(
                Wrappers.lambdaQuery(ConversationMessageDO.class)
                        .eq(ConversationMessageDO::getConversationId, conversationId)
                        .eq(ConversationMessageDO::getDelFlag, 0)
                        .orderByAsc(ConversationMessageDO::getCreateTime)
        );

        //转VO
        List<ConversationMessageVO> conversationMessageVOList = conversationMessageDOList.stream()
                .map(doObj -> ConversationMessageVO.builder()
                        .id(doObj.getId().toString())
                        .conversationId(doObj.getConversationId().toString())
                        .userId(doObj.getUserId().toString())
                        .role(doObj.getRole())
                        .content(doObj.getContent())
                        .createTime(doObj.getCreateTime())
                        .delFlag(doObj.getDelFlag())
                        .build()
                )
                .collect(Collectors.toList());

        return conversationMessageVOList;
    }

    @Override
    public void saveConversationMessage(String conversationId, String userId, String role, String content) {
        ConversationMessageDO conversationMessageDO = ConversationMessageDO
                .builder()
                .conversationId(Long.parseLong(conversationId))
                .userId(Long.parseLong(userId))
                .role(role)
                .content(content)
                .build();
        conversationMessageMapper.insert(conversationMessageDO);
    }

    @Override
    public void deleteByMessageId(DeleteMessageRequest request) {
        Assert.notNull(request, ()-> new ClientException("请求体不能为空"));


        conversationMessageMapper.update(
                Wrappers.<ConversationMessageDO>lambdaUpdate()
                        .eq(ConversationMessageDO::getId, request.getMessageId())
                        .eq(ConversationMessageDO::getRole, request.getRole())
                .set(ConversationMessageDO::getDelFlag, 1)
        );
        //缓存的del_flag也要修改
    }


}
