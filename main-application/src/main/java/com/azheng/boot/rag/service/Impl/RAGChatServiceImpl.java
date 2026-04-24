package com.azheng.boot.rag.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.rag.config.LLMConfig;
import com.azheng.boot.rag.core.intent.IntentGroup;
import com.azheng.boot.rag.core.intent.IntentResolver;
import com.azheng.boot.rag.core.intent.SubQuestionIntent;
import com.azheng.boot.rag.core.memory.AbstractCompressService;
import com.azheng.boot.rag.core.memory.HistoryAbstractStore;
import com.azheng.boot.rag.core.prompt.PromptContext;
import com.azheng.boot.rag.core.prompt.PromptTemplateLoader;
import com.azheng.boot.rag.core.prompt.RAGPromptService;
import com.azheng.boot.rag.core.retrieval.RetrievalContext;
import com.azheng.boot.rag.core.retrieval.RetrievalEngine;
import com.azheng.boot.rag.core.rewrite.QueryTransformerService;
import com.azheng.boot.rag.core.rewrite.RewriteResult;
import com.azheng.boot.rag.service.ChatDataPersistenceService;
import com.azheng.boot.rag.service.ConversationService;
import com.azheng.boot.rag.service.RAGChatService;
import com.azheng.framework.context.UserContext;
import com.azheng.framework.exception.ServiceException;
import com.esotericsoftware.minlog.Log;
import com.github.benmanes.caffeine.cache.Cache;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.azheng.boot.rag.constant.RAGConstant.*;

/* RAG模块 */
@Service
@RequiredArgsConstructor
public class RAGChatServiceImpl implements RAGChatService {

    private final LLMConfig llmConfig;
    private final RAGPromptService ragPromptService;
    private final QueryTransformerService queryTransformerService;
    private final IntentResolver intentResolver;
    private final RetrievalEngine retrievalEngine;
    private final ChatDataPersistenceService chatDataPersistenceService;
    private final ConversationService conversationService;
    private final PromptTemplateLoader promptTemplateLoader;
    private final AbstractCompressService abstractCompressService;
    private final HistoryAbstractStore historyAbstractStore;

    @Qualifier("streamHandleCache")
    private final Cache<String, StreamingHandle> streamHandleCache;

    @Qualifier("originalChatMemoryProvider")
    private final ChatMemoryProvider originalChatMemoryProvider;

    @Qualifier("treatResponseThreadPoolExecutor")
    private final Executor treatResponseThreadPoolExecutor;

    @Override
    public void streamingChat(String userQuestion , String roundId, String conversationId, Boolean openThinking, SseEmitter emitter) {

        // 获取当前会话用户
        String userId = UserContext.getUserId();

        // final保证会话Id只被赋值一次
        final String actualConversationId;

        // 构造首次请求的会话Id
        boolean isFirstChat = StrUtil.isBlank(conversationId);
        actualConversationId = isFirstChat?IdUtil.getSnowflakeNextIdStr():conversationId;


        // 首帧推送 conversationId 给前端
        if (isFirstChat) {
            try {
                emitter.send(SseEmitter.event()
                        .name("init") // 事件名：init
                        .data("{\"conversationId\":\"" + actualConversationId + "\"}") //数据
                );
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }

        // 按条件构造本次聊天模型
        StreamingChatModel streamingChatModel = llmConfig.buildQwenFlashStreamingModel(openThinking);

        // 根据会话id获取会话信息缓存
        ChatMemory chatMemory = originalChatMemoryProvider.get(actualConversationId);
        List<ChatMessage> historyMessages = chatMemory.messages();
        // 获取当前会话记忆的摘要
        List<ChatMessage> abstractMessages = historyAbstractStore.query(historyMessages);
        if(CollUtil.isNotEmpty(abstractMessages)){
            historyMessages.addAll(abstractMessages);
        }



        // 构造对话元数据（如会话Id、业务场景、会话记忆）
        UserMessage userMessage = UserMessage.from(userQuestion);
        InvocationContext invocationContext = InvocationContext
                                .builder()
                                .chatMemoryId(actualConversationId)
                                .build();
        Metadata metadata = Metadata
                                .builder()
                                .chatMessage(userMessage)
                                .chatMemory(historyMessages)
                                .invocationContext(invocationContext)
                                .build();

        // 创建查询对象
        Query query = Query.from(userQuestion, metadata);

        /**
         * 1.用户查询改写
         */
        RewriteResult rewriteResult = queryTransformerService.buildRewriteResult(query);

        /**
         * 2.意图识别
         */
        List<SubQuestionIntent> subQuestionIntentList = intentResolver.resolve(rewriteResult);

        /**
         * 意图判断：
         * 1.纯SYS -> true
         * 2.部分SYS、部分无意图 ->true
         * 3.掺杂KB意图 ->false
         */
        boolean allSystemOnly = subQuestionIntentList.stream()
                .allMatch(si -> intentResolver.isSystemOnly(si.nodeScores()));

        // 全是系统交互意图
        if (allSystemOnly) {
            //1.获取意图节点携带的系统交互提示词模版
            String customPrompt = subQuestionIntentList.stream()
                    .flatMap(si -> si.nodeScores().stream())
                    .map(ns -> ns.getNode().getPromptTemplate())
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse(null);
            // 2.封装最终提示词
            String systemPrompt = StrUtil.isNotBlank(customPrompt)
                    ? customPrompt
                    : promptTemplateLoader.load(CHAT_SYSTEM_PROMPT_TEMPLATE_PATH);

            List<ChatMessage> chatMessages = new ArrayList<>();
            chatMessages.add(SystemMessage.from(systemPrompt));

            if(CollUtil.isNotEmpty(historyMessages)){
                chatMessages.addAll(historyMessages);
            }
            chatMessages.add(UserMessage.from(userQuestion));

            TreatRequest request = TreatRequest
                    .builder()
                    .userId(userId)
                    .userQuestion(userQuestion)
                    .chatMemory(chatMemory)
                    .isFirstChat(isFirstChat)
                    .build();

            // 3.模型响应
            LLMChatResponse(streamingChatModel, chatMessages, emitter, roundId , actualConversationId, request);

            return;
        }


        /**
         * 3.检索
         */
        RetrievalContext retrievalContext = retrievalEngine.retrieval(subQuestionIntentList, DEFAULT_TOP_K);

        if(retrievalContext.isEmpty()){
            String replay = "未检索到相关的文档内容";
            try {
                emitter.send(replay);
                emitter.complete();
                return;
            } catch (IOException e) {
                throw new ServiceException("RAG检索为空，提前返回出现异常："+e);
            }
        }

        //聚合分组的意图
        IntentGroup mergedGroup = intentResolver.mergeIntentGroup(subQuestionIntentList);

        //构造检索上下文
        PromptContext promptContext = PromptContext.builder()
                .question(rewriteResult.rewrittenQuestion())
                .kbContext(retrievalContext.getKbContext())
                .kbIntents(mergedGroup.kbIntents())
                .intentChunks(retrievalContext.getRecallChunks())
                .build();

        /**
         * 4.prompt组装
         */
        List<ChatMessage> messages = ragPromptService.buildStructuredMessages(promptContext,
                historyMessages,
                userQuestion,
                rewriteResult.subQuestions()
        );

        /**
         * 5.调用聊天服务
         */
        TreatRequest request = TreatRequest
                .builder()
                .userId(userId)
                .userQuestion(userQuestion)
                .chatMemory(chatMemory)
                .isFirstChat(isFirstChat)
                .build();
        LLMChatResponse(streamingChatModel, messages, emitter, roundId , actualConversationId, request);

    }

    @Override
    public void cancel(String roundId) {
        StreamingHandle streamingHandle = streamHandleCache.getIfPresent(roundId);
        if (streamingHandle == null) {
            Log.warn("对话："+roundId+" 流式取消句柄为空，可能已完成或已取消");
            return;
        }
        streamingHandle.cancel();
        streamHandleCache.invalidate(roundId);
        Log.warn("对话："+roundId+"终止");
    }


    /**
     * 调用模型对话服务
     * 记录模型输出内容，做进一步处理
     */

    private void LLMChatResponse(StreamingChatModel streamingChatModel,
                                 List<ChatMessage> messages,
                                 SseEmitter emitter,
                                 String roundId,
                                 String actualConversationId,
                                 TreatRequest treatRequest) {
        // 模型服务
        streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialThinking(PartialThinking partialThinking, PartialThinkingContext context) {
                //返回思考内容
                try {
                    emitter.send(partialThinking.text());
                    streamHandleCache.put(roundId,context.streamingHandle());
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
                onPartialThinking(partialThinking);
            }



            @Override
            public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                //追加流式句柄至本地缓存
                StreamingHandle present = streamHandleCache.getIfPresent(roundId);
                if(present == null){
                    streamHandleCache.put(roundId,context.streamingHandle());
                }

                //1.流式输出回答
                try {
                    emitter.send(partialResponse.text());
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 1.返回推理过程
                try {
                    String text = completeResponse.aiMessage().text();
                    // 收到成功响应文本直接开始异步收尾
                    CompletableFuture.runAsync(() ->
                        treatResponse(actualConversationId, treatRequest.getUserId(), treatRequest.getUserQuestion(), text, treatRequest.getChatMemory(), treatRequest.getIsFirstChat())
                                ,treatResponseThreadPoolExecutor
                    );


                    String thinkingText = completeResponse.aiMessage().thinking();
                    if (StrUtil.isNotBlank(thinkingText)) {
                        emitter.send("【模型推理过程】："+"\n"+thinkingText);
                    }

                    emitter.complete();


                } catch (IOException e) {
                    emitter.completeWithError(e);
                }finally {
                    //完成之后删除本轮流式输出句柄缓存
                    streamHandleCache.invalidate(roundId);
                }
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
                streamHandleCache.invalidate(roundId);
            }
        });
        // 防止刚发起对话就关闭浏览器
        emitter.onCompletion(() -> streamHandleCache.invalidate(roundId));
        //超时
        emitter.onTimeout(() -> streamHandleCache.invalidate(roundId));
        //意外错误断开连接..
        emitter.onError(e -> streamHandleCache.invalidate(roundId));
    }


    /**
     * 负责模型成功响应之后的收尾工作
     * @param actualConversationId 会话Id
     * @param userId 用户Id
     * @param userQuestion 原始用户提问
     * @param responseText 模型响应文本
     * @param chatMemory 会话记忆
     *                   持久化
     */
    public void treatResponse(String actualConversationId,
                               String userId,
                               String userQuestion,
                               String responseText,
                               ChatMemory chatMemory,
                               Boolean isFirstChat){
        // 1.MySQL持久化消息
        try {
            chatDataPersistenceService.mysqlPersistent(actualConversationId,userId,userQuestion,responseText,isFirstChat);

            // 2.多轮对话压缩检测器
            abstractCompressService.CompressIfNeed(actualConversationId,chatMemory.messages());

            // 3.缓存本轮对话
            UserMessage userMessage = UserMessage.from(userQuestion);
            AiMessage aiMessage = AiMessage.from(responseText);
            chatMemory.add(userMessage);
            chatMemory.add(aiMessage);

            // 4.首次->生成标题（变更：根据原始用户提问+模型响应text生成）
            conversationService.generateTitle(actualConversationId,userQuestion);
        } catch (Exception e) {
            Log.error("\n"+"本轮对话收尾工作失败:"+
                    "\n"+"用户提问："+userQuestion+
                    "\n"+"异常信息：",e);
            throw new ServiceException("会话："+actualConversationId+" 收尾阶段出现异常"+e);
        }


    }

    @Data
    @Builder
    public static class TreatRequest{
        private String userId;
        private String userQuestion;
        private ChatMemory chatMemory;
        private Boolean isFirstChat;
    }


}
