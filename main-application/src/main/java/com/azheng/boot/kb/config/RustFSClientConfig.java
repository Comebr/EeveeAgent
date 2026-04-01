package com.azheng.boot.kb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class RustFSClientConfig {

    /**
     * RustFSClient
     * Spring容器创建时会自动扫描有@Bean注解的方法，然后调用方法，将返回的对象自动创建
     */
    @Bean(name = "RustFSClient")
    public S3Client RustFSClient() {
        return S3Client.builder()
                // 配置SDK应与其通信的端点：配置RustFS服务的url
                .endpointOverride(URI.create("http://192.168.191.128:9001"))
                // 配置区域 中国北部-1
                .region(Region.CN_NORTH_1)
                // 配置用于与AWS认证的凭证,就是配置账号密码，可以是动态的token，也可以是我们设置的静态账号密码
                .credentialsProvider(
                 StaticCredentialsProvider.create(
                 AwsBasicCredentials.create("rustfsadmin", "rustfsadmin")))
                // 强制该客户端对桶使用路径式寻址。
                .forcePathStyle(true)
                .build();
    }
}
