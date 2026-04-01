package com.azheng.boot.kb.controller;

import com.azheng.boot.kb.controller.request.CreateKbRequest;
import com.azheng.boot.kb.controller.request.KnowledgeBasePageRequest;
import com.azheng.boot.kb.controller.request.ReNameKbRequest;
import com.azheng.boot.kb.service.KnowledgeBaseService;
import com.azheng.boot.kb.controller.vo.KnowledgeBaseVO;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kb")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService kbService;


    @PostMapping("/createKb")
    public Result<Boolean> createKnowledgeBase(@RequestBody CreateKbRequest request) {
        return Results.success(kbService.create(request));
    }

    @DeleteMapping("/deleteKb/{kb-id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable("kb-id") String kbID){
        kbService.delete(kbID);
        return Results.success();
    }

    @PutMapping("/renameKb/{kb-id}")
    public Result<Void> updateKnowledgeBase(@PathVariable("kb-id") String kbID,
                                            @RequestBody ReNameKbRequest request){
        kbService.update(kbID, request);
        return Results.success();
    }

    /**
     * 分页查询知识库列表
     */
    @GetMapping("/pageQuery")
    public Result<IPage<KnowledgeBaseVO>> pageQuery(KnowledgeBasePageRequest requestParam) {
        return Results.success(kbService.pageQuery(requestParam));
    }
}
