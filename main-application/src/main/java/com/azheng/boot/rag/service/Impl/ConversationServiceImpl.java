package com.azheng.boot.rag.service.Impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.rag.controller.vo.ConversationMessageVO;
import com.azheng.boot.rag.controller.vo.ConversationVO;
import com.azheng.boot.rag.core.prompt.PromptTemplateLoader;
import com.azheng.boot.rag.dao.entity.ConversationDO;
import com.azheng.boot.rag.dao.entity.ConversationMessageDO;
import com.azheng.boot.rag.dao.mapper.ConversationMapper;
import com.azheng.boot.rag.dao.mapper.ConversationMessageMapper;
import com.azheng.boot.rag.service.ConversationMessageService;
import com.azheng.boot.rag.service.ConversationService;
import com.azheng.boot.user.mapper.UserMapper;
import com.azheng.boot.user.po.UserDO;
import com.azheng.framework.context.UserContext;
import com.azheng.framework.exception.ClientException;
import com.azheng.framework.exception.ServiceException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.azheng.boot.rag.constant.RAGConstant.TITLE_SUMMARY_TEMPLATE_PATH;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private ConversationMessageService conversationMessageService;

    @Resource
    private PromptTemplateLoader promptTemplateLoader;

    @Resource(name = "qwen-turbo")
    private ChatModel chatModel;

    @Override
    public List<ConversationVO> queryConversations(String userId) {
        //校验用户id合法性
        String currentUserId = UserContext.getUserId();
        if (!currentUserId.equals(userId)) {
            throw new ClientException("请求用户与当前用户不一致！");
        }
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new ClientException("当前请求用户信息不存在！");
        }
        if (userDO.getStatus() == 0) {
            throw new ClientException("当前请求用户已禁用");
        }
        List<ConversationDO> conversationDOList = conversationMapper.selectList(
                Wrappers.lambdaQuery(ConversationDO.class)
                        .eq(ConversationDO::getUserId, userId)
                        .eq(ConversationDO::getDelFlag, 0)
                        .orderByDesc( ConversationDO::getLastTalkTime)
                        .orderByDesc(ConversationDO::getCreateTime)
        );
        //转VO
        List<ConversationVO> conversationVOList = conversationDOList.stream()
                .map(doObj -> ConversationVO.builder()
                        .id(doObj.getId().toString())
                        .conversationId(doObj.getConversationId().toString())
                        .userId(doObj.getUserId().toString())
                        .title(doObj.getTitle())
                        .lastTalkTime(doObj.getLastTalkTime())
                        .delFlag(doObj.getDelFlag())
                        .build()
                )
                .collect(Collectors.toList());

        return conversationVOList;
    }

    @Override
    public void mkdirTitle(String newTitle, String conversationId) {
        String title = StrUtil.trimToNull(newTitle);
        //去除两边空白符后的字符串
        Assert.notNull(title, () -> new ClientException("会话标题不能为空"));

        conversationMapper.update(
                Wrappers.<ConversationDO>lambdaUpdate()
                        .eq(ConversationDO::getConversationId, conversationId)
                        .eq(ConversationDO::getDelFlag, 0)
                        .set(ConversationDO::getTitle, title)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByConversationId(String conversationId) {
        ConversationDO conversationDO = conversationMapper.selectOne(
                Wrappers.lambdaQuery(ConversationDO.class)
                        .eq(ConversationDO::getConversationId, conversationId)
                        .eq(ConversationDO::getDelFlag, 0)
        );
        if (conversationDO == null) {
            throw new ClientException("非法的会话请求id");
        }
        try {
            conversationMapper.update(
                    Wrappers.lambdaUpdate(ConversationDO.class)
                    .eq(ConversationDO::getConversationId, conversationId)
                    .set(ConversationDO::getDelFlag, 1)
            );
            conversationMessageMapper.update(
                    Wrappers.lambdaUpdate(ConversationMessageDO.class)
                    .eq(ConversationMessageDO::getConversationId, conversationId)
                    .set(ConversationMessageDO::getDelFlag, 1)
            );
            //缓存的del_flag也要修改
        } catch (Exception e) {
            throw new ServiceException("删除会话失败,会话ID：" + conversationId);
        }
    }

    @Override
    public List<ConversationMessageVO> queryConversationMessage(String conversationId) {
        return conversationMessageService.queryConversationMessage(conversationId);
    }

    @Override
    public void generateTitle(String conversationId, String userQuestion) {
        //首次对话完成 → 判断消息数 = 2 → 异步调用轻量 LLM → 生成 15 字内标题 → 更新会话表 → 永久不再自动变

        ConversationDO conversationDO = conversationMapper.selectOne(Wrappers.<ConversationDO>lambdaQuery()
                .eq(ConversationDO::getConversationId, conversationId)
                .eq(ConversationDO::getDelFlag, 0)
        );

        // 0.校验当前会话标题
        if(conversationDO.getTitle().equals("新对话")) {
            String title ;
            try {
                // 1.生成提示词
                String titlePrompt = promptTemplateLoader.render(TITLE_SUMMARY_TEMPLATE_PATH,
                                                                Map.of("question", userQuestion)
                );
                // 2.LLM生成摘要
                title = chatModel.chat(titlePrompt);
                title = StrUtil.trimToNull(title);
            } catch (Exception e) {
                log.warn("标题摘要：模型调用出现异常", e);
                // 降级兜底策略：截取前15个
                title = StrUtil.sub(userQuestion.trim(), 0, 15) + "...";

            }

            // 3.修改标题
            conversationMapper.update(
                    Wrappers.<ConversationDO>lambdaUpdate()
                    .eq(ConversationDO::getConversationId, conversationId)
                    .set(ConversationDO::getTitle, title)
            );
        }
    }
}
