package com.azheng.boot.rag.controller;


import com.azheng.boot.rag.controller.request.SampleQuestionCreateRequest;
import com.azheng.boot.rag.controller.request.SampleQuestionPageRequest;
import com.azheng.boot.rag.controller.request.SampleQuestionUpdateRequest;
import com.azheng.boot.rag.controller.vo.SampleQuestionVO;
import com.azheng.boot.rag.service.SampleQuestionService;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 示例问题控制器（欢迎页展示）
 */
@RestController
@RequestMapping("/example")
@RequiredArgsConstructor
public class SampleQuestionController {

    private final SampleQuestionService sampleQuestionService;

    /**
     * 随机获取示例问题列表
     */
    @GetMapping("/rag/sample-questions")
    public Result<List<SampleQuestionVO>> listSampleQuestions() {
        return Results.success(sampleQuestionService.listRandomQuestions());
    }

    /**
     * 分页查询示例问题列表
     */
    @GetMapping("/sample-questions")
    public Result<IPage<SampleQuestionVO>> pageQuery(SampleQuestionPageRequest requestParam) {
        return Results.success(sampleQuestionService.pageQuery(requestParam));
    }

    /**
     * 查询示例问题详情
     */
    @GetMapping("/sample-questions/{id}")
    public Result<SampleQuestionVO> queryById(@PathVariable String id) {
        return Results.success(sampleQuestionService.queryById(id));
    }

    /**
     * 创建示例问题
     */
    @PostMapping("/sample-questions")
    public Result<String> create(@RequestBody SampleQuestionCreateRequest requestParam) {
        return Results.success(sampleQuestionService.create(requestParam));
    }

    /**
     * 更新示例问题
     */
    @PutMapping("/sample-questions/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody SampleQuestionUpdateRequest requestParam) {
        sampleQuestionService.update(id, requestParam);
        return Results.success();
    }

    /**
     * 删除示例问题
     */
    @DeleteMapping("/sample-questions/{id}")
    public Result<Void> delete(@PathVariable String id) {
        sampleQuestionService.delete(id);
        return Results.success();
    }
}
