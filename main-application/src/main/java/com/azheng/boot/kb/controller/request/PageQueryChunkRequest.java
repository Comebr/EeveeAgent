package com.azheng.boot.kb.controller.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class PageQueryChunkRequest extends Page {

    /**
     * 所属文档Id
     */
    private String docId;

    /**
     * 启用状态
     */
    private String enabled;

    /**
     * 知识块内容模糊匹配
     */
    private String chunkText;
}
