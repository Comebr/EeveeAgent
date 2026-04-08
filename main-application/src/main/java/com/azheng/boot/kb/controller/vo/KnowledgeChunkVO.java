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
public class KnowledgeChunkVO {

    private String id;
    private Integer ChunkIndex;
    private String ChunkText;
    private Integer enabled;
    private Integer charCount;
    private Integer tokenCount;
    private Date updateTime;
}
