package com.azheng.boot.rag.core.embedding.milvus.operation;

import cn.hutool.core.lang.Assert;
import com.azheng.boot.rag.core.embedding.milvus.config.MilvusClientConfig;
import com.azheng.framework.exception.ClientException;
import com.azheng.framework.exception.ServiceException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.database.request.CreateDatabaseReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Milvus通用操作
 */
@Slf4j
@Component
public class MilvusOperations {

    @Value(value = "${langchain4j.community.dashscope.dimensions}")
    private Integer dimension;

    @Resource
    private MilvusClientV2 milvusClientV2;

    @Resource
    private EmbeddingModel embeddingModel;


    /**
     * 创建数据库
     */
    public void createDataBase(String dbName) {
        Assert.notNull(dbName,()->new ServiceException("Milvus数据库名称不能为空"));

        // 校验数据库名称是否重复
        List<String> databaseNames = milvusClientV2.listDatabases().getDatabaseNames();
        boolean isExist = databaseNames.contains(dbName);
        if (isExist) {
            log.error("Milvus数据库名称已存在，请重新输入名称");
            throw new ClientException("Milvus数据库名称已存在，请重新输入名称");
        }


        //构造CreateDatabaseReq (properties不用重新配置，服务端的配置文件中会自动应用)
        CreateDatabaseReq createDatabaseReq = CreateDatabaseReq
                .builder()
                .databaseName(dbName)
                .build();
        //需要参数CreateDatabaseReq
            milvusClientV2.createDatabase(createDatabaseReq);

    }


    /**
     * 创建collection(该方法仅适用于存储知识块表)
     * @param collectionName
     */
    public boolean createChunkCollection(String collectionName) {
        Assert.notNull(collectionName,()->new ServiceException("Milvus<UNK>"));

        // 1.校验collectionName唯一性
        ensureCollectionNotExist(collectionName, milvusClientV2);


        // 2.创建Schema
        CreateCollectionReq.CollectionSchema schema = milvusClientV2.createSchema();
        // id
        schema.addField(AddFieldReq
                        .builder()
                        .fieldName("chunk_id")
                        .dataType(DataType.Int64)
                        .isPrimaryKey(true)
                        .autoID(true)
                        .build());
        //向量文本
        schema.addField(AddFieldReq
                        .builder()
                        .fieldName("chunk_text_vector")
                        .dataType(DataType.FloatVector)
                        .dimension(dimension)
                        .maxLength(512)
                        .build());
        //字符文本(长度限制不要卡太死)
        schema.addField(AddFieldReq
                        .builder()
                        .fieldName("chunk_text_varchar")
                        .dataType(DataType.VarChar)
                        .maxLength(dimension)
                        .build());

        // 3.配置索引
        IndexParam indexParamForChunkId = IndexParam.builder()
                .fieldName("chunk_id")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .build();

        IndexParam indexParamForVector = IndexParam.builder()
                .fieldName("chunk_text_vector")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.COSINE)
                .build();
        List<IndexParam> indexParams = Arrays.asList(indexParamForChunkId, indexParamForVector);

        // 4.构造CollectionSchema
        CreateCollectionReq collectionReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(indexParams)
                .build();

        try {
            // 5.创建Collection
            milvusClientV2.createCollection(collectionReq);

            // 6.返回加载状态
            Boolean loadState = milvusClientV2.getLoadState(GetLoadStateReq.builder().collectionName(collectionName).build());
            return loadState;
        } catch (Exception e) {
            log.error("创建Collection失败，请检查Milvus客户端");
            throw new ServiceException("创建Collection失败，请检查Milvus客户端");
        }
    }

    /**
     * 删除记录
     */
    public void deleteEntity(Long milvusId,String collectionName) {
        milvusClientV2.delete(DeleteReq
                .builder()
                .collectionName(collectionName)
                .ids(Arrays.asList(milvusId))
                .build());
    }

    /**
     * 插入实体
     */

    /**
     * 修改记录（底层不可变、采用删除新增）
     */
    public void updateEntity(Long milvusId, String collectionName,String chunkText) {
        TextSegment textSegment = TextSegment.from(chunkText);
        List<JsonObject> data = new ArrayList<>();
        /// 1.构造实体
        // 文本向量化
        Embedding embedding = embeddingModel.embed(textSegment).content();
        List<Float> vector = embedding.vectorAsList();
        String text = textSegment.text();

        // 构造JsonObject
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("chunk_id", milvusId);
        jsonObject.add("chunk_text_vector",new Gson().toJsonTree(vector));
        jsonObject.addProperty("chunk_text_varchar", text);
        data.add(jsonObject);



        /// 2.修改
        milvusClientV2.upsert(UpsertReq.builder()
                                .collectionName(collectionName)
                                .data(data)
                                .build()
        );
    }

    /**
     * 查询记录
     */

    /**
     * 校验collectionName唯一性
     * @param collectionName
     * @param milvusClient
     */
    private static void ensureCollectionNotExist(String collectionName, MilvusClientV2 milvusClient) {
        List<String> collectionNames = milvusClient.listCollections().getCollectionNames();
        boolean contains = collectionNames.contains(collectionName);
        if (contains) {
            log.error("Collection名称已存在");
            throw new ClientException("Collection名称已存在");
        }
    }


}
