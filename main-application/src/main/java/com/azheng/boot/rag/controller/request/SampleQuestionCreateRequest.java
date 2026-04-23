package com.azheng.boot.rag.controller.request;

import lombok.Data;

@Data
public class SampleQuestionCreateRequest{
    /**
     * 展示标题（可选）
     */
    private String title;

    /**
     * 描述或提示（可选）
     */
    private String description;

    /**
     * 示例问题内容
     */
    private String question;
}
