package com.azheng.boot.kb.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("knowledge_base")
public class KnowledgeBasePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 知识库名称
     */
    private String kbName;

    /**
     * 向量模型名
     */
    private String embeddingModel;

    /**
     * Milvus Collection 名称（创建后禁止修改）可以把它想象成传统 SQL 数据库中的表格
     */
    private String collection;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private String updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    /**
     * 删除标志，默认0：启用 1：弃用
     */
    private Integer delFlag;
}
