package com.azheng.boot.kb.controller;

import com.azheng.boot.kb.controller.request.MkdirChunkTextRequest;
import com.azheng.boot.kb.controller.request.PageQueryChunkRequest;
import com.azheng.boot.kb.controller.request.PageQueryDocByKbIdRequest;
import com.azheng.boot.kb.controller.request.StartChunkRequest;
import com.azheng.boot.kb.controller.vo.KnowledgeChunkVO;
import com.azheng.boot.kb.controller.vo.KnowledgeDocumentVO;
import com.azheng.boot.kb.service.KnowledgeChunkService;
import com.azheng.boot.kb.service.KnowledgeDocumentService;
import com.azheng.framework.exception.ServiceException;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/kb/doc/chunk")
public class KnowledgeChunkController {

    @Resource
    private KnowledgeChunkService knowledgeChunkService;
    /**
     * 根据kbId，分页查询所属文档列表
     */
    @GetMapping("/pageQuery")
    public Result<IPage<KnowledgeChunkVO>> queryByDocId(PageQueryChunkRequest request) {
        try {
            IPage<KnowledgeChunkVO> knowledgeChunkVOIPage = knowledgeChunkService.pageQuery(request);
            return Results.success(knowledgeChunkVOIPage);
        } catch (Exception e) {
            throw new ServiceException("知识块分页查询失败");
        }
    }

    @PutMapping("/mkdirText")
    public Result<Void> updateText(@RequestBody MkdirChunkTextRequest request){
        try {
            knowledgeChunkService.updateChunkText(request);
            return Results.success();
        } catch (Exception e) {
            throw new ServiceException("手动维护知识块内容失败");
        }
    }

    /**
     * 根据Id删除分块
     */
    @DeleteMapping("/deleteChunk/{id}")
    public Result<Void> deleteDoc(@PathVariable("id") String chunkId){
        knowledgeChunkService.remove(chunkId);
        return Results.success();
    }

}
