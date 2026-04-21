package com.azheng.boot.rag.controller;

import com.azheng.boot.rag.controller.request.MkdirTitleRequest;
import com.azheng.boot.rag.controller.vo.ConversationMessageVO;
import com.azheng.boot.rag.controller.vo.ConversationVO;
import com.azheng.boot.rag.service.ConversationService;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Resource
    private ConversationService conversationService;

    @GetMapping("/list")
    public Result<List<ConversationVO>> query(@RequestParam String userId){
        return Results.success(conversationService.queryConversations(userId));
    }

    @PostMapping("/mkdir")
    public Result<Void> updateTitle(@RequestBody MkdirTitleRequest request){
        try {
            conversationService.mkdirTitle(request.getTitle(),request.getConversationId());
            return Results.success();
        } catch (Exception e) {
            return Results.failure();
        }
    }

    @PutMapping("remove")
    public Result<Void> delete(@RequestParam String conversationId){
        try {
            conversationService.deleteByConversationId(conversationId);
            return Results.success();
        } catch (Exception e) {
            return Results.failure();
        }
    }

    @GetMapping("/{conversationId}")
    public Result<List<ConversationMessageVO>> openConversationById(@PathVariable String conversationId){
        return Results.success(conversationService.queryConversationMessage(conversationId));
    }
}
