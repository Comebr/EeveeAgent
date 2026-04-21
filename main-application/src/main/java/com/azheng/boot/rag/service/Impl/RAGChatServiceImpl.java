package com.azheng.boot.rag.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.rag.config.LLMConfig;
import com.azheng.boot.rag.core.intent.IntentGroup;
import com.azheng.boot.rag.core.intent.IntentResolver;
import com.azheng.boot.rag.core.intent.SubQuestionIntent;
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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.azheng.boot.rag.constant.RAGConstant.*;

/* RAG模块 */
@Service
public class RAGChatServiceImpl implements RAGChatService {

    @Resource
    private LLMConfig llmConfig;

    @Resource
    private ChatMemoryProvider chatMemoryProvider;

    @Resource
    private RAGPromptService ragPromptService;

    @Resource
    private QueryTransformerService queryTransformerService;

    @Resource
    private IntentResolver intentResolver;

    @Resource
    private RetrievalEngine retrievalEngine;

    @Resource
    private ChatDataPersistenceService chatDataPersistenceService;

    @Resource
    private ConversationService conversationService;

    @Resource
    private PromptTemplateLoader promptTemplateLoader;

    private ConcurrentHashMap<String,Boolean> cancellationFlags = new ConcurrentHashMap<>();

    @Override
    public void streamingChat(String userQuestion , String conversationId, Boolean openThinking, SseEmitter emitter) {

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
        ChatMemory chatMemory = chatMemoryProvider.get(actualConversationId);
        List<ChatMessage> historyMessages = chatMemory.messages();



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

        // 意图判断
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

            // 3.模型响应
            CompletableFuture<String> response = LLMChatResponse(streamingChatModel, chatMessages, emitter, actualConversationId);

            // 4.持久化
            response.whenComplete((result, throwable) -> {
                if(throwable != null){
                    return;
                }
                treatResponse(actualConversationId,userId,userQuestion,result,chatMemory,isFirstChat);
            });
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
        CompletableFuture<String> response = LLMChatResponse(streamingChatModel, messages, emitter, actualConversationId);

        /**
         * 6.收尾
         * 不阻塞Tomcat线程,异步聊天完成后，自动调用treatResponse收尾
         */
        response.whenComplete((result, throwable) -> {
            if(throwable != null){
                return;
            }
            treatResponse(actualConversationId,userId,userQuestion,result,chatMemory,isFirstChat);
        });
    }


    /**
     * 调用模型对话服务
     * 记录模型输出内容，做进一步处理
     *
     * @return
     */

    private CompletableFuture<String> LLMChatResponse(StreamingChatModel streamingChatModel,
                                                      List<ChatMessage> messages,
                                                      SseEmitter emitter,
                                                      String actualConversationId) {

        // 1. 创建异步结果（核心：自动承载返回值 + 同步等待）
        CompletableFuture<String> future = new CompletableFuture<>();

        cancellationFlags.put(actualConversationId, Boolean.FALSE);
        emitter.onCompletion(()-> cancellationFlags.remove(actualConversationId));
        emitter.onTimeout(()-> cancellationFlags.remove(actualConversationId));
        emitter.onError((e)-> cancellationFlags.remove(actualConversationId));


        streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                // 检查取消标志
                if (Boolean.TRUE.equals(cancellationFlags.get(actualConversationId))) {
                    // 取消流式处理
                    if (context.streamingHandle() != null) {
                        context.streamingHandle().cancel();
                    }
                    try {
                        emitter.send("【已取消】");
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                    future.completeExceptionally(new ServiceException("用户取消对话"));
                    return;
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
                    String thinkingText = completeResponse.aiMessage().thinking();
                    if (StrUtil.isNotBlank(thinkingText)) {
                        emitter.send("【模型推理过程】："+"\n"+thinkingText);
                    }
                    String responseText = completeResponse.aiMessage().text();

                    emitter.complete();

                    future.complete(responseText);

                } catch (IOException e) {
                    emitter.completeWithError(e);
                    future.completeExceptionally(e);
                }finally {
                    cancellationFlags.remove(actualConversationId);
                }
            }

            @Override
            public void onError(Throwable error) {
                cancellationFlags.remove(actualConversationId);
                emitter.completeWithError(error);
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    // 添加取消方法
    public void cancelChat(String conversationId) {
        cancellationFlags.put(conversationId, true);
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
    private void treatResponse(String actualConversationId,
                               String userId,
                               String userQuestion,
                               String responseText,
                               ChatMemory chatMemory,
                               Boolean isFirstChat){

        // 1.MySQL
        chatDataPersistenceService.mysqlPersistent(actualConversationId,userId,userQuestion,responseText,isFirstChat);

        // 2.用户消息缓存-Redis
        chatMemory.add( UserMessage.from(userQuestion));

        // 3.保存ai消息至记忆缓存
        chatMemory.add(AiMessage.from(responseText));

        // 4.首次-生成标题（变更：根据原始用户提问+模型响应text生成）
        conversationService.generateTitle(actualConversationId,userQuestion);
    }



}
