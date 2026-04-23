package com.azheng.boot.rag.service;

import com.azheng.boot.rag.controller.request.SampleQuestionCreateRequest;
import com.azheng.boot.rag.controller.request.SampleQuestionPageRequest;
import com.azheng.boot.rag.controller.request.SampleQuestionUpdateRequest;
import com.azheng.boot.rag.controller.vo.SampleQuestionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface SampleQuestionService {

    /**
     * 创建示例问题
     */
    String create(SampleQuestionCreateRequest requestParam);

    /**
     * 更新示例问题
     */
    void update(String id, SampleQuestionUpdateRequest requestParam);

    /**
     * 删除示例问题
     */
    void delete(String id);

    /**
     * 查询示例问题详情
     */
    SampleQuestionVO queryById(String id);

    /**
     * 分页查询示例问题列表
     */
    IPage<SampleQuestionVO> pageQuery(SampleQuestionPageRequest requestParam);

    /**
     * 随机获取示例问题列表
     */
    List<SampleQuestionVO> listRandomQuestions();
}