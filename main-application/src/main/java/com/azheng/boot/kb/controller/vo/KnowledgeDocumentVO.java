package com.azheng.boot.kb.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentVO {

    private String id;
    private String kbId;
    private String documentName;
    private String fileType;
    private Long fileSize;
    private String createBy;
    private Date updateTime;
    private Integer enabled;
}
