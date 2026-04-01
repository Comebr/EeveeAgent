package com.azheng.boot.kb.service;

import com.azheng.boot.kb.controller.request.PageQueryDocByKbIdRequest;
import com.azheng.boot.kb.controller.vo.KnowledgeDocumentVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeDocumentService {
    /**
     * 根据kbId，分页查询所属文档列表
     */
    IPage<KnowledgeDocumentVO> docPageQuery(PageQueryDocByKbIdRequest request);


    /**
     * 文件上传upload
     *
     */
    void uploadFile(MultipartFile file, String kbId);

    /**
     * 文档删除
     */
    void deleteFile(String docId);

    /**
     * TODO 前端拉杆做二次弹窗确认，加上点击延迟，避免重复调接口
     * 前端拉杆 开 → 传 1，拉杆 关 → 传 0后端直接更新成前端传的值，不做任何取反！
     * 修改文档启用状态
     */
    void setEnabled(String docId, boolean enabled);


    /**
     * 编辑文档信息
     * 目前只支持修改数据库中存的documentName
     */


    /**
     * 进行文档分块
     */

    /**
     * 文件下载到本地
     */
}

