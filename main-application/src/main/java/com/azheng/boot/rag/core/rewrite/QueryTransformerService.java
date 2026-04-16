package com.azheng.boot.rag.core.rewrite;

import com.azheng.framework.exception.ServiceException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;


/**
 * lc4j-rag-query-transform
 * 用户提问：精简+扩展
 */
@Slf4j
@Service
public class QueryTransformerService {

    @Resource(name = "qwen-turbo")
    private ChatModel retrievalToolModel;

    /**
     * 基于对话上下文压缩扩展
     * 可配置参数：
     * -chatModel
     * -promptTemplate 当前使用的都是官方默认的提示词模版
     */
    public Query queryCompressRewrite(Query userQuery) {
        //默认不会主动获取对话上下文，但它会自动检测并使用 Query.metadata() 中的 chatMemory（对话记忆）（如果存在）进行查询压缩。
        Collection<Query> queries = null;
        try {
            CompressingQueryTransformer compressingQueryTransformer = CompressingQueryTransformer
                    .builder()
                    .chatModel(retrievalToolModel)
                    .build();
            queries = compressingQueryTransformer.transform(userQuery);
            Query query = queries.stream().toList().get(0);

            log.info("》查询改写阶段：压缩重写成功" +"\n"
                    +"——————————>："+ query.text());

            return query;

        } catch (Exception e) {
            log.warn("》查询改写阶段：基于对话上下文压缩扩展失败 {}",e.getMessage());
            throw new ServiceException("lc4j-rag-query-transform：压缩用户提问失败");
        }

    }

    /**
     * 多角度扩展查询转换器：只产生子问题
     * 可配置参数：
     * -chatModel
     * -promptTemplate
     * -n(扩展数量，默认3)
     */
    public List<Query> queryExpandingRewrite(Query userQuery) {
        Collection<Query> queries = null;
        try {
            ExpandingQueryTransformer expandingQueryTransformer = ExpandingQueryTransformer
                    .builder()
                    .chatModel(retrievalToolModel)
                    .build();
            queries = expandingQueryTransformer.transform(userQuery);
        } catch (Exception e) {
            throw new ServiceException("lc4j-rag-query-transform：扩展查询失败");
        }
        List<Query> list = queries.stream().toList();
        return list;
    }

    /**
     * 根据用户提问封装最终重写结果
     * @param query
     * @return
     */
    public RewriteResult buildRewriteResult(Query query){
        //获取精简版问题
        String rewrittenQuestion = queryCompressRewrite(query).text();

        //获取扩展的子问题
        List<String> subQuestions = queryExpandingRewrite(query).stream().map(Query::text).toList();

        return new RewriteResult(rewrittenQuestion, subQuestions);
    }

}
