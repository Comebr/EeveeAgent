package com.azheng.agent.service.Impl;

import com.azheng.agent.service.ChatService;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public class ChatServiceImpl implements ChatService {

    /**
     * 引入系统默认聊天模型
     */
    @Resource
    private StreamingChatModel streamingChatModel;

    @Override
    public void streamingChat(String message, SseEmitter emitter) {
        streamingChatModel.chat(message, new StreamingChatResponseHandler() {

            /**
             * 单个token发送
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    emitter.send(partialResponse);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            /**
             * 当模型完成响应流式传输时调用。
             */
            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                emitter.complete();
            }

            /**
             * 当流媒体传输过程中发生错误时，会调用该方法。
             */
            @Override
            public void onError(Throwable throwable) {
                emitter.completeWithError(throwable);
            }
        });
    }
}
