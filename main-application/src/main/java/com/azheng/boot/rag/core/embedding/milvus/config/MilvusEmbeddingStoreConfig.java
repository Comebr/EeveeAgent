package com.azheng.boot.rag.core.embedding.milvus.config;

import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class MilvusEmbeddingStoreConfig {

    @Value("${milvus.dbname}")
    private String dbName;

    @Value("${langchain4j.community.dashscope.dimensions}")
    private Integer dimension;

    public MilvusEmbeddingStore creteMilvusEmbeddingStore(String collectionName) {
        return MilvusEmbeddingStore.builder()
                .host("192.168.191.128")                         // Host for Milvus instance
                .port(19530)                               // Port for Milvus instance
                .dimension(dimension)                            // Dimension of vectors
                .databaseName(dbName)
                .collectionName(collectionName)
                .indexType(IndexType.AUTOINDEX)                 // Index type
                .metricType(MetricType.COSINE)             // Metric type
                .idFieldName("chunk_id")                         // ID field name
                .textFieldName("chunk_text_varchar")                     // Text field name
                .vectorFieldName("chunk_text_vector")                 // Vector field name
                .metadataFieldName("metadata")
//                .username("username")                      // Username for Milvus
//                .password("password")                      // Password for Milvus
//                .consistencyLevel(ConsistencyLevelEnum.EVENTUALLY)  // Consistency level
//                .autoFlushOnInsert(true)                   // Auto flush after insert
//                .metadataFieldName("metadata")             // Metadata field name
                .build();                                  // Build the MilvusEmbeddingStore instance
    }
}
