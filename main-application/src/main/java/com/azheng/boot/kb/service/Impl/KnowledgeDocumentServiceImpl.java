package com.azheng.boot.kb.service.Impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.kb.controller.request.PageQueryDocByKbIdRequest;
import com.azheng.boot.kb.controller.request.StartChunkRequest;
import com.azheng.boot.kb.controller.vo.KnowledgeDocumentVO;
import com.azheng.boot.kb.dao.mapper.FileTORustFSLogMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeBaseMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeChunkMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeDocumentMapper;
import com.azheng.boot.kb.dao.po.FileToRustFSLogPO;
import com.azheng.boot.kb.dao.po.KnowledgeBasePO;
import com.azheng.boot.kb.dao.po.KnowledgeChunkPO;
import com.azheng.boot.kb.dao.po.KnowledgeDocumentPO;
import com.azheng.boot.kb.service.FileValidationService;
import com.azheng.boot.kb.service.KnowledgeDocumentService;
import com.azheng.framework.context.UserContext;
import com.azheng.framework.exception.ClientException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    public static final String BUCKET_NAME = "a-bucket";

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Resource(name = "RustFSClient")
    private S3Client RustFsClient;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private FileTORustFSLogMapper fileTORustFSLogMapper;

    @Resource
    private knowledgeChunkServiceImpl knowledgeChunkServiceImpl;

    /**
     * 查询知识库下的文档
     */
    @Override
    public IPage<KnowledgeDocumentVO> docPageQuery(PageQueryDocByKbIdRequest request) {
        // 非空校验
        Assert.notNull(request, "请求不能为空");
        String kbId = StrUtil.trimToNull(request.getKbId());
        String documentName = request.getDocumentName();
        String enabled = request.getEnabled();
        // 构造Page对象
        Page<KnowledgeDocumentPO> page = new Page<>(request.getCurrent(), request.getSize());
        QueryWrapper<KnowledgeDocumentPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("kb_id", kbId);
        queryWrapper.eq("del_flag", 0);
        if(enabled != null ){
            queryWrapper.eq("enabled", Integer.parseInt(enabled));
        }
        if(StrUtil.isNotBlank(documentName) || documentName != null) {
            queryWrapper.like("document_name", documentName);
        }
        queryWrapper.orderByDesc("update_time");
        // 查询结果
        Page<KnowledgeDocumentPO> result = knowledgeDocumentMapper.selectPage(page, queryWrapper);
        // 封装VO返回
        KnowledgeDocumentVO knowledgeDocumentVO = new KnowledgeDocumentVO();
        BeanUtils.copyProperties(result, knowledgeDocumentVO);
        return result.convert(this::toVO);

    }


    /**
     * 文件上传：
     * 1.文件准入校验
     * 2.文件上传至RustFS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadFile(MultipartFile file, String kbId){
        // 1.文件准入校验
        try {
            FileValidationService.validateFile(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        KnowledgeBasePO knowledgeBasePO = knowledgeBaseMapper.selectById(kbId);
        Assert.notNull(knowledgeBasePO ,() -> new ClientException("知识库为空！"));
        String filename = file.getOriginalFilename();
        // 2.生成唯一key
        UUID uuid = UUID.randomUUID();
        String key = "uploadFile/"+uuid+"/"+filename;
        // 3.构建putObjectRequest
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .contentType(file.getContentType())
                .build();
        // 4.将InputStream传给RustFS
        try(InputStream inputStream = file.getInputStream()){
            RustFsClient.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            // 5.存储记录进MySQL（fileUrl暂时不设置）
            KnowledgeDocumentPO documentPO = KnowledgeDocumentPO
                    .builder()
                    .documentName(filename)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .kbId(Long.valueOf(kbId))
                    .createBy(UserContext.getUsername())
                    .build();
            knowledgeDocumentMapper.insert(documentPO);

            // 6.记录文件映射
            FileToRustFSLogPO rustFSLogPO = FileToRustFSLogPO.builder()
                    .docId(documentPO.getId())
                    .rustfsKey(key)
                    .build();
            fileTORustFSLogMapper.insert(rustFSLogPO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件删除
     */
    @Override
    public void deleteFile(String docId) {
        //文档表记录删除
        knowledgeDocumentMapper.update(Wrappers
                                        .<KnowledgeDocumentPO>lambdaUpdate()
                                        .eq(KnowledgeDocumentPO::getId, docId)
                                        .set(KnowledgeDocumentPO::getDel_flag,1)
        );
        //归属知识块删除
        knowledgeChunkMapper.update(Wrappers
                                    .<KnowledgeChunkPO>lambdaUpdate()
                                    .eq(KnowledgeChunkPO::getDocId, docId)
                                    .set(KnowledgeChunkPO::getDel_flag,1)
        );
    }

    /**
     * 启用or禁用
     */
    @Override
    public void setEnabled(String docId, boolean enabled) {
        KnowledgeDocumentPO knowledgeDocumentPO = knowledgeDocumentMapper.selectById(docId);
        Assert.notNull(knowledgeDocumentPO ,() -> new ClientException("未发现对应文档信息"));
        int newStatus = enabled ? 1 : 0;
        knowledgeDocumentPO.setEnabled(newStatus);
        knowledgeDocumentPO.setUpdateBy(UserContext.getUsername());
        knowledgeDocumentMapper.updateById(knowledgeDocumentPO);
    }

    /**
     * 启动分块
     * @param startChunkRequest
     */
    @Override
    public void startChunking(StartChunkRequest startChunkRequest) {
        knowledgeChunkServiceImpl.performChunking(startChunkRequest);
    }

    private KnowledgeDocumentVO toVO(KnowledgeDocumentPO knowledgeDocumentPO) {
        return KnowledgeDocumentVO.builder()
                .id(knowledgeDocumentPO.getId().toString())
                .kbId(knowledgeDocumentPO.getKbId().toString())
                .documentName(knowledgeDocumentPO.getDocumentName())
                .fileType(knowledgeDocumentPO.getFileType())
                .fileSize(knowledgeDocumentPO.getFileSize())
                .createBy(knowledgeDocumentPO.getCreateBy())
                .updateTime(knowledgeDocumentPO.getUpdateTime())
                .enabled(knowledgeDocumentPO.getEnabled())
                .build();
    }
}
