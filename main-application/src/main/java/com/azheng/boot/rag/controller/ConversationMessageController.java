package com.azheng.boot.rag.controller;


import com.azheng.boot.rag.controller.request.DeleteMessageRequest;
import com.azheng.boot.rag.service.ConversationMessageService;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
public class ConversationMessageController {

    @Resource
    private ConversationMessageService conversationMessageService;

    @PutMapping("remove")
    public Result<Void> delete(@RequestBody DeleteMessageRequest request) {
        try {
            conversationMessageService.deleteByMessageId(request);
            return Results.success();
        } catch (Exception e) {
            return Results.failure();
        }
    }
}
