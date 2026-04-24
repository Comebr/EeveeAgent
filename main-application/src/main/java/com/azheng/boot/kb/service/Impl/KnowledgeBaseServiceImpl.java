package com.azheng.boot.kb.service.Impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.kb.dao.mapper.KnowledgeBaseMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeChunkMapper;
import com.azheng.boot.kb.dao.mapper.KnowledgeDocumentMapper;
import com.azheng.boot.kb.dao.po.KnowledgeBasePO;
import com.azheng.boot.kb.controller.request.CreateKbRequest;
import com.azheng.boot.kb.controller.request.KnowledgeBasePageRequest;
import com.azheng.boot.kb.controller.request.ReNameKbRequest;
import com.azheng.boot.kb.dao.po.KnowledgeChunkPO;
import com.azheng.boot.kb.dao.po.KnowledgeDocumentPO;
import com.azheng.boot.kb.service.KnowledgeBaseService;
import com.azheng.boot.kb.controller.vo.KnowledgeBaseVO;
import com.azheng.boot.rag.core.embedding.milvus.operation.MilvusOperations;
import com.azheng.framework.context.UserContext;
import com.azheng.framework.exception.ClientException;
import com.azheng.framework.exception.ServiceException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static cn.dev33.satoken.SaManager.log;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Resource
    private MilvusOperations milvusOperations;

    /**
     * 创建知识库
     * 目前配置的embeddingModel，是用于所属知识块向量化时做模型匹配的
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(CreateKbRequest request) {
        Assert.notNull(request,()-> new ClientException("知识库创建请求不能为空"));
        String kbName = StrUtil.trimToNull(request.getKbName());
        String embeddingModelName = StrUtil.trimToNull(request.getEmbeddingModel());
        String collectionName = StrUtil.trimToNull(request.getCollection());
        Assert.notNull(kbName,()-> new ClientException("知识库名称不能为空"));
        Assert.notNull(embeddingModelName,()-> new ClientException("embedding模型名不能为空"));
        Assert.notNull(collectionName,()-> new ClientException("collection表名不能为空"));

        KnowledgeBasePO knowledgeBasePO = KnowledgeBasePO.builder()
                .kbName(kbName)
                .embeddingModel(embeddingModelName)
                .collection(collectionName)
                .createBy(UserContext.getUsername())
                .build();
        try {
            //存入MySQL
            int i = knowledgeBaseMapper.insert(knowledgeBasePO);

            //创建Collection
            boolean loaded = milvusOperations.createChunkCollection(request.getCollection());
            if(i<=0 || !loaded){
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 删除知识库
     * 暂时逻辑删除，后续做定时归档+清理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String kbID) {
        Long kbId = Long.parseLong(kbID);

        // 1.删除MySQL记录
        knowledgeBaseMapper.update(Wrappers
                                    .<KnowledgeBasePO>lambdaUpdate()
                                    .eq(KnowledgeBasePO::getId, kbId)
                                    .set(KnowledgeBasePO::getDelFlag,1)
        );

        // 2.删除所属文档
        knowledgeDocumentMapper.update(Wrappers.<KnowledgeDocumentPO>lambdaUpdate()
                .eq(KnowledgeDocumentPO::getKbId,kbId)
                .set(KnowledgeDocumentPO::getDelFlag,1)
        );
        // 3.删除所属知识块
        knowledgeChunkMapper.update(Wrappers.<KnowledgeChunkPO>lambdaUpdate()
                .eq(KnowledgeChunkPO::getKbId,kbId)
                .set(KnowledgeChunkPO::getDelFlag,1)
        );

        // 4.删除所属MilvusCollection
    }

    /**
     * 重命名知识库
     */
    @Override
    public void update(String kbId, ReNameKbRequest request) {
        KnowledgeBasePO kb = knowledgeBaseMapper.selectById(Long.parseLong(kbId));
        if (kb == null || (kb.getDelFlag() != null && kb.getDelFlag() == 1)) {
            throw new ClientException("知识库不存在");
        }

        if (!StringUtils.hasText(request.getKbName())) {
            throw new ClientException("知识库名称不能为空");
        }
        // 去除用户输入的所用空格
        String kbName = request.getKbName().replaceAll("\\s+", "");
        // 名称重复校验
        Long count = knowledgeBaseMapper.selectCount(
                Wrappers.lambdaQuery(KnowledgeBasePO.class)
                        .eq(KnowledgeBasePO::getKbName, kbName)
                        //（排除当前知识库）
                        .ne(KnowledgeBasePO::getId, Long.parseLong(kbId))
                        .eq(KnowledgeBasePO::getDelFlag, 0)
        );
        if (count > 0) {
            throw new ServiceException("知识库名称已存在：" + request.getKbName());
        }

        kb.setKbName(request.getKbName());
        kb.setUpdateBy(UserContext.getUsername());
        knowledgeBaseMapper.updateById(kb);

        log.info("成功重命名知识库, kbId={}, newName={}", kbId, request.getKbName());

    }
    @Override
    public IPage<KnowledgeBaseVO> pageQuery(KnowledgeBasePageRequest requestParam) {
        String kbName = StrUtil.trimToNull(requestParam.getKbName());
        Page<KnowledgeBasePO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        Page<KnowledgeBasePO> result = knowledgeBaseMapper.selectPage(page,
                Wrappers.lambdaQuery(KnowledgeBasePO.class)
                        .eq(KnowledgeBasePO::getDelFlag, 0)
                        .and(StrUtil.isNotBlank(kbName), wrapper -> wrapper
                                .like(KnowledgeBasePO::getKbName, kbName))
                        .orderByDesc(KnowledgeBasePO::getUpdateTime)
        );
        KnowledgeBaseVO knowledgeBaseVO = new KnowledgeBaseVO();
        BeanUtils.copyProperties(result, knowledgeBaseVO);
        return result.convert(this::toVO);
    }

    private KnowledgeBaseVO toVO(KnowledgeBasePO knowledgeBasePO) {
        return KnowledgeBaseVO.builder()
                .id(knowledgeBasePO.getId().toString())
                .kbName(knowledgeBasePO.getKbName())
                .embeddingModel(knowledgeBasePO.getEmbeddingModel())
                .collection(knowledgeBasePO.getCollection())
                .createBy(knowledgeBasePO.getCreateBy())
                .createTime(knowledgeBasePO.getCreateTime())
                .updateTime(knowledgeBasePO.getUpdateTime())
                .build();
    }
}
