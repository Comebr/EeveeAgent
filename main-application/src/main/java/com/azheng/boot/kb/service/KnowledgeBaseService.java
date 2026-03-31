package com.azheng.boot.kb.service;

import com.azheng.boot.kb.request.CreateKbRequest;
import com.azheng.boot.kb.request.KnowledgeBasePageRequest;
import com.azheng.boot.kb.request.ReNameKbRequest;
import com.azheng.boot.kb.vo.KnowledgeBaseVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface KnowledgeBaseService {

    /**
     * 创建知识库
     */
    boolean create(CreateKbRequest request);

    /**
     * 删除知识库
     */
    void delete(String kbID);

    /**
     * 修改知识库名称
     */
    void update(String kbID, ReNameKbRequest request);

    IPage<KnowledgeBaseVO> pageQuery(KnowledgeBasePageRequest requestParam);
}
