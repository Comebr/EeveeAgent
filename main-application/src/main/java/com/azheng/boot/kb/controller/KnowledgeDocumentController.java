package com.azheng.boot.kb.controller;

import com.azheng.boot.kb.controller.request.PageQueryDocByKbIdRequest;
import com.azheng.boot.kb.controller.request.StartChunkRequest;
import com.azheng.boot.kb.controller.vo.KnowledgeDocumentVO;
import com.azheng.boot.kb.service.KnowledgeDocumentService;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/kb/doc")
public class KnowledgeDocumentController {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    /**
     * 本地单文件上传
     */
    @PostMapping("/upload")
    public Result<Void> upload(@RequestParam("file") MultipartFile file, @RequestParam("kbId") String kbId) {
        try {
            knowledgeDocumentService.uploadFile(file, kbId);
        } catch (Exception e) {
            return Results.failure();
        }
        return Results.success();
    }


    /**
     * 根据kbId，分页查询所属文档列表
     */
    @GetMapping("/pageQuery")
    public Result<IPage<KnowledgeDocumentVO>> queryByKbId(PageQueryDocByKbIdRequest request) {
        return Results.success(knowledgeDocumentService.docPageQuery(request));
    }

    /**
     * 根据Id删除文档
     */
    @DeleteMapping("/deleteDoc/{id}")
    public Result<Void> deleteDoc(@PathVariable("id") String docId){
        knowledgeDocumentService.deleteFile(docId);
        return Results.success();
    }

    /**
     * 启用开关
     */
    @PostMapping("/switch")
    public Result<Void> updateEnabled(String docId, boolean enabled){
        knowledgeDocumentService.setEnabled(docId, enabled);
        return Results.success();
    }

    /**
     * 启动分块
     */
    @PostMapping("/startChunk")
    public Result<Void> runChunk(@RequestBody StartChunkRequest startChunkRequest){
        knowledgeDocumentService.startChunking(startChunkRequest);
        return Results.success();
    }
}
