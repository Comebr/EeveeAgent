package com.azheng.boot.kb.controller.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 知识库VO
 * 传给前端的数据类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeBaseVO {
    private String id;
    private String kbName;

    private String embeddingModel;

    private String collection;

    private String createBy;

    private Date createTime;

    private Date updateTime;
}
