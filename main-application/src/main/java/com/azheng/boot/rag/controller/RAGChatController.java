package com.azheng.boot.rag.controller;


import com.azheng.boot.rag.service.RAGChatService;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/agent/rag")
public class RAGChatController {
    @Resource
    private RAGChatService ragChatService;

    @GetMapping("/streamingChat")
    public SseEmitter streamingChat(@RequestParam("prompt") String question,
                                    @RequestParam(required = false) String conversationId,
                                    @RequestParam(required = false,defaultValue = "false") Boolean openThinking){
        SseEmitter sseEmitter = new SseEmitter(3 * 60 * 1000L);
        ragChatService.streamingChat(question, conversationId, openThinking, sseEmitter);
        return sseEmitter;
    }


    // 停止接口
    @PostMapping("/cancel")
    public Result<Void> cancelChat(@RequestParam String conversationId) {
        ragChatService.cancelChat(conversationId);
        return Results.success();
    }
}
