package com.azheng.boot.rag.core.embedding.milvus.operation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.InsertResp;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 运用官方Milvus客户端向量化
 */
@Component
public class VectorEmbedding {

    /**
     * 向量模型
     */
    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private MilvusClientV2 milvusClientV2;



    /**
     * 向量化存储: 支持单条或多条
     * @return
     */
    public List<Object> embed(List<TextSegment> textSegments, String collectionName) {
        List<JsonObject> data = new ArrayList<>();
        for (TextSegment textSegment : textSegments) {
            /// 1.构造实体
            // 文本向量化
            Embedding embedding = embeddingModel.embed(textSegment).content();
            List<Float> vector = embedding.vectorAsList();
            String text = textSegment.text();

            // 构造JsonObject
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("chunk_text_vector",new Gson().toJsonTree(vector));
            jsonObject.addProperty("chunk_text_varchar", text);

            // ===================== 【核心：添加 Metadata】 =====================
            // 1. 从 TextSegment 中获取已设置的 Metadata（非null）
            Metadata metadata = textSegment.metadata();
            // 2. 将 Metadata 转为 Map，再转成 JsonObject
            Map<String, Object> metadataMap = metadata.toMap();
            JsonObject metadataJson = new Gson().toJsonTree(metadataMap).getAsJsonObject();
            // 3. 添加到 Milvus 数据中（字段名必须是 metadata）
            jsonObject.add("metadata", metadataJson);

            data.add(jsonObject);
        }


        // 链接Milvus客户端操作
        /// 2.插入实体
        InsertResp insert = milvusClientV2.insert(InsertReq
                                            .builder()
                                            .collectionName(collectionName)
                                            .data(data)
                                            .build());
        /// 3.返回主键
        return insert.getPrimaryKeys();
    }
}
