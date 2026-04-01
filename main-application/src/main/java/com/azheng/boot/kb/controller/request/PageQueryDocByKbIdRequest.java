package com.azheng.boot.kb.controller.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class PageQueryDocByKbIdRequest extends Page {
    /**
     * 查询指定知识库下的文档列表
     */
    private String kbId;

    /**
     * 姓名模糊查询
     */
    private String documentName;

    /**
     * 状态：0：禁用 1：启用 null:全部
     */
    private String enabled;
}
