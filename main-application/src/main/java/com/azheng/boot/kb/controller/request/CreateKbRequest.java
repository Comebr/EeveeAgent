package com.azheng.boot.kb.controller.request;


import lombok.Data;

/**
 * 前端创建知识库请求体
 */
@Data
public class CreateKbRequest {

    //知识库名称
    private String kbName;

    //向量模型名
    private String embeddingModel;

    //collection表名
    private String collection;
}
