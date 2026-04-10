package com.azheng.boot.rag.embedding.milvus.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus官方SDK连接配置
 */
@Configuration
public class MilvusClientConfig {
    @Value("${milvus.uri}")
    private String uri;

    @Value("${milvus.dbname}")
    private String dbName;

    @Value("${milvus.connectTimeout:5000}")
    private Long connectTimeout;

    @Bean(destroyMethod = "close")
    public MilvusClientV2 connect() {
        return new MilvusClientV2(
                ConnectConfig
                        .builder()
                        .uri(uri)
                        .dbName(dbName)
                        .connectTimeoutMs(connectTimeout)
                        .idleTimeoutMs(6000)
                        .keepAliveTimeMs(3000)
                        .build()
        );
    }
}
