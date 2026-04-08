package com.azheng.boot.kb.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_chunk")
public class KnowledgeChunkPO {


    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 所属文档ID
     */
    private Long docId;

    /**
     * 分块序号
     */
    private Integer chunkIndex;

    /**
     * 分块正文内容
     */
    private String chunkText;

    /**
     * 字符数
     */
    private Integer charCount;

    /**
     * token数
     */
    private  Integer tokenCount;

    /**
     * 是否启用 0：禁用 1：启用
     */
    private Integer enabled;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer del_flag;
}
