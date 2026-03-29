package com.azheng.boot.chat;

import com.azheng.agent.service.ChatService;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/agent")
public class ChatController {

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private ChatService chatService;

    @GetMapping("/ceshi/streamingChat")
    public SseEmitter streamingChat(@RequestParam(value = "prompt") String prompt){
        SseEmitter sseEmitter = new SseEmitter();
        chatService.streamingChat(prompt, sseEmitter);
        return sseEmitter;
    }

}