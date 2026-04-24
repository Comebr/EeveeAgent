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
                                    @RequestParam String roundId,
                                    @RequestParam(required = false) String conversationId,
                                    @RequestParam(required = false,defaultValue = "false") Boolean openThinking){
        SseEmitter sseEmitter = new SseEmitter(3 * 60 * 1000L);
        ragChatService.streamingChat(question, roundId , conversationId, openThinking, sseEmitter);
        return sseEmitter;
    }


    @PostMapping("/cancel")
    public Result<Void> streamingChatCancellation(@RequestParam String roundId){
        try {
            ragChatService.cancel(roundId);
            return Results.success();
        } catch (Exception e) {
            return Results.failure();
        }
    }



}
