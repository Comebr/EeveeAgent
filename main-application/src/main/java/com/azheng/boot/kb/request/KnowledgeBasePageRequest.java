package com.azheng.boot.kb.request;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class KnowledgeBasePageRequest extends Page {

    /**
     * 按名称模糊匹配
     */
    private String kbName;
}
