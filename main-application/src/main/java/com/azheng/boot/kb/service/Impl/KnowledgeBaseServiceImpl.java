package com.azheng.boot.kb.service.Impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.azheng.boot.kb.dao.mapper.KnowledgeBaseMapper;
import com.azheng.boot.kb.dao.po.KnowledgeBasePO;
import com.azheng.boot.kb.controller.request.CreateKbRequest;
import com.azheng.boot.kb.controller.request.KnowledgeBasePageRequest;
import com.azheng.boot.kb.controller.request.ReNameKbRequest;
import com.azheng.boot.kb.service.KnowledgeBaseService;
import com.azheng.boot.kb.controller.vo.KnowledgeBaseVO;
import com.azheng.framework.context.UserContext;
import com.azheng.framework.exception.ClientException;
import com.azheng.framework.exception.ServiceException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static cn.dev33.satoken.SaManager.log;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 创建知识库
     */
    @Override
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
            int i = knowledgeBaseMapper.insert(knowledgeBasePO);
            if(i<=0){
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 删除知识库
     */
    @Override
    public void delete(String kbID) {
        //TODO 文档部分做好之后，删除之前要确保改知识库下无附属文档

        knowledgeBaseMapper.deleteById(Long.parseLong(kbID));
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
