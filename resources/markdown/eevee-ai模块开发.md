# eevee-ai模块开发

## 一、前言

> 项目初期：基于langchain4j实现全流程,后期引入SpringAI、SAA来重构
>
> 注意：eevee-ai层不写启动类、配置文件，以及HTTP接口暴露Controller，交给main-application实现

### 1.引入相关依赖

父工程依赖

```java
            
<langchain4j.version>1.11.0</langchain4j.version>
			<dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j-bom</artifactId>
                <version>${langchain4j.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j-community-bom</artifactId>
                <version>1.11.0-beta19</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
```

AI子工程依赖

```java
        <!--1. OpenAI 模型自动配置（包含了 langchain4j 核心主包和 langchain4j-open-ai 基础包）-->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
        </dependency>
        <!--2 AI Services 高阶支持（@AiService、工具调用、会话记忆、RAG 自动装配）-->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-spring-boot-starter</artifactId>
        </dependency>

        <!-- 3. 通义千问模型接入 -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-community-dashscope-spring-boot-starter</artifactId>
        </dependency>
```



接下来在main-application工程中引入eevee-ai依赖

```java
        <!--继承AI模块依赖-->
        <dependency>
            <groupId>com.atazheng-study</groupId>
            <artifactId>eevee-ai</artifactId>
            <version>${project.version}</version>
        </dependency>
```





### 2.模型选择

application.yaml配置：系统模型：ChatModel+Embedding

方案一：OpenAi兼容国内模型

方案二：DashScope阿里千问（免配置base_url）

暂时选择方案一：

```yaml
#---百炼平台-通义千问核心配置---
langchain4j:
  #OpenAI
  open-ai:
    #流式输出模型 qwen-flash
    streaming-chat-model:
      api-key: ${BAILIAN_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-flash

    #文本向量模型 text-embedding-v4
    embedding-model:
      api-key: ${BAILIAN_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: text-embedding-v4

```

配置类：其他模型





### 3.实现简单的流式输出聊天对话

##### 01.Impl

```java
package com.azheng.agent.service.Impl;

import com.azheng.agent.service.ChatService;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
public class ChatServiceImpl implements ChatService {

    /**
     * 引入系统默认聊天模型
     */
    @Resource
    private StreamingChatModel streamingChatModel;

    @Override
    public void streamingChat(String message, SseEmitter emitter) {
        streamingChatModel.chat(message, new StreamingChatResponseHandler() {

            /**
             * 单个token发送
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    emitter.send(partialResponse);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            /**
             * 当模型完成响应流式传输时调用。
             */
            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                emitter.complete();
            }

            /**
             * 当流媒体传输过程中发生错误时，会调用该方法。
             */
            @Override
            public void onError(Throwable throwable) {
                emitter.completeWithError(throwable);
            }
        });
    }
}

```



##### 02.service

```java
package com.azheng.agent.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    //流式聊天对话接口

    void streamingChat(String message, SseEmitter emitter);
}

```



##### 03.Controller



```java
package com.azheng.boot.chat;

import com.azheng.agent.service.ChatService;
import dev.langchain4j.model.chat.StreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/agent")
public class ChatController {

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private ChatService chatService;

    @GetMapping("/ceshi/streamingChat")
    public SseEmitter streamingChat(@RequestParam(value = "prompt") String prompt){
        SseEmitter sseEmitter = new SseEmitter();
        chatService.streamingChat(prompt, sseEmitter);
        return sseEmitter;
    }

}
```



本次采用的实现方式还是采用传统的三层架构方式，相比于AiService 接口生成代理类代码量更多，但能实现更加复杂的逻辑（三层架构可能都要被迭代了..........）





## 二.RAG全流程构建

### 1.知识库底座开发

> 用户的垂直领域知识是大模型所不具备的，大模型预训练的数据都是通识，对于特定领域，模型没有详细的参考，因此构建知识库是整个RAG的基层，也称index阶段

#### 1.1 知识入库流程分析

1. 文件上传：用户上传文件
2. 文件解析：使用Apache Tika 提取纯文本
3. 文件切割：先查doc、再分chunk，入库
4. 向量化：调用本地的Embedding-Model将知识块转化为Vector
5. 双端存储：MySQL持久化文本、Milvus持久化向量



知识库结构梳理：

知识库

|——文档1

|		|——若干chunk

|——文档2

|		|——若干chunk

|——....



#### 1.2 知识库管理

##### 知识库表结构：

```sql
CREATE TABLE `t_knowledge_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识库名称',
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '嵌入模型标识',
  `collection_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Milvus Collection',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建人',
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0：正常 1：删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_name` (`collection_name`) COMMENT 'Collection 唯一约束',
  KEY `idx_kb_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2018586042835763201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识库表';
```



##### 创建KnowledgeBasePO

```java
package com.azheng.agent.kb.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("knowledge_base")
public class KnowledgeBasePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 知识库名称
     */
    private String kbName;
    
    /**
     * 向量模型名
     */
    private String embeddingModel;
    
    /**
     * Milvus Collection 名称（创建后禁止修改）
     */
    private String collection;
    
    private String createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    private String updateBy;
    
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
    
    /**
     * 删除标志，默认0：启用 1：弃用
     */
    private Integer delFlag;
}

```

##### Mapper层：

实现接口继承BaseMapper<KnowledgeBasePO>

```java

public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBasePO> {
}

```

主启动类配置MapperScan路径



```java
@MapperScan(basePackages ={
        "com.azheng.boot.user.mapper",
        "com.azheng.boot.kb.dao.mapper"})
```





##### service层

​	





#### 1.3 构建文档管理

难点：文档上传、文档解析

##### 	文档表结构

文件核心字段：文件名、文件类型、文件地址、文件大小

```sql
CREATE TABLE `knowledge_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `kb_id` bigint NOT NULL COMMENT '所属知识库ID',
  `document_name` varchar(255) NOT NULL COMMENT '文档名称',
  `file_type` varchar(32) NOT NULL COMMENT '文件类型（如txt、pdf、word、md....）',
  `file_url` varchar(1024) NOT NULL COMMENT '文件地址',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（单位：字节）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用 0：禁用 1：启用',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0：正常 1：删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库-文档表';
```



##### 建PO类：



```java
package com.azheng.boot.kb.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_document")
public class KnowledgeDocumentPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long kbId;

    /**
     * 文档名称/文件名
     */
    private String documentName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 文件大小（单位字节）
     */
    private  Long fileSize;

    /**
     * 是否启用 0：禁用 1：启用
     */
    private Integer enabled;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer del_flag;
}

```



##### 写service接口

|     功能     | 接口名       |               参数                |       具体描述       |
| :----------: | ------------ | :-------------------------------: | :------------------: |
|   文件上传   | uploadFile   |        MultipartFile file         |  将文件上传到RustFS  |
|   文件删除   | deleteFile   |           String docId            |  删除文档（逻辑删）  |
| 文件状态修改 | setEnabled   |   String docId, boolean enabled   |     启用状态开关     |
|   文件查询   | docPageQuery | PageQueryDocByKbIdRequest request | 分页查询所属文档列表 |
|   文件下载   |              |                                   |                      |
|   文件编辑   |              |                                   |                      |



##### 建Request类

前端->Controller

| 类名              | 字段                        |
| ----------------- | --------------------------- |
| uploadFileRequest | MultipartFile multipartFile |
|                   |                             |
|                   |                             |
|                   |                             |
|                   |                             |



##### 建VO类：

Controller->前端

| 类名 | 字段 |
| ---- | ---- |
|      |      |
|      |      |
|      |      |
|      |      |
|      |      |



##### 写Mapper

```java
package com.azheng.boot.kb.dao.mapper;

import com.azheng.boot.kb.dao.po.KnowledgeDocumentPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentPO> {
}
```



##### 写ServiceImpl

```java
    /**
     * 文件上传
     */
    @Override
    public void uploadFile(MultipartFile file, String kbId) {
        // 1.空值校验
        Assert.notNull(file ,() -> new ClientException("文件不能为空！"));
        Assert.notNull(kbId ,() -> new ClientException("知识库ID不能为空！"));
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
```



##### 写Controller

```java
    @PostMapping("/upload")
    public Result<Void> upload(@RequestParam("file") MultipartFile file, @RequestParam("kbId") String kbId) {
        try {
            knowledgeDocumentService.uploadFile(file, kbId);
        } catch (Exception e) {
            return Results.failure();
        }
        return Results.success();
    }
```













#### 1.4.构建知识块表

得到的文档要交有分割器去分割成若干个知识块chunk

chunk核心字段：分块的序号、分块的内容、知识块的字符数、token数



```sql
CREATE TABLE `knowledge_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `kb_id` bigint NOT NULL COMMENT '所属知识库ID',
  `doc_id` bigint NOT NULL COMMENT '所属文档ID',
  `chunk_index` int NOT NULL COMMENT '分块序号（从0开始）',
  `chunk_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分块正文内容',
  `char_count` int DEFAULT NULL COMMENT '知识块的字符数',
  `token_count` int DEFAULT NULL COMMENT '知识块的token数',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用 0：禁用 1：启用',
  `status` bigint NOT NULL DEFAULT '1' COMMENT '启用状态：0：禁用，1：启用',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0：正常 1：删除',
  PRIMARY KEY (`id`),
  KEY `idx_kb_id` (`kb_id`) USING BTREE,
  KEY `idx_doc_id` (`doc_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```









### 2. 文档预处理



分析流程：

1.文档准入校验：格式白名单、检测恶意文件、损坏、空、加密文件

2.文件上传至RustFS

3.Tika文档解析：识别文件类型、统一解析、提取出纯文本内容

4,原始文本初级清理：剔除无意义格式符号、统一编码

5.深度文本清洗：水印、版权声明、广告，去重、错别字

6.文本结构化与语义规整

7.元数据提取

8.文本切片：按语义边界切分或按固定长度

9.分块后质检过滤：过滤空、短、噪声块

10.向量化前置处理：文本归一化、超长内容截断、提交给Embedding模型



#### 2.1 文件注入校验

前端：文件格式和大小检查

后端：com.azheng.boot.kb.service.FileValidationService







#### 2.2 文件上传

```java
/**
 * 文件上传：
 * 1.文件准入校验
 * 2.文件上传至RustFS
 */
@Override
public void uploadFile(MultipartFile file, String kbId){
    // 1.文件准入校验
    try {
        FileValidationService.validateFile(file);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    Assert.notNull(kbId ,() -> new ClientException("知识库ID不能为空！"));
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
```



#### 2.3 文档解析

引入Apache Tika 依赖

```xml
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
</dependency>

<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
</dependency>
```



```java
//核心代码：
    private static final Tika TIKA = new Tika();
	String text = TIKA.parseToString(byteArrayInputStream);
```

- 文档解析的核心就是：parseToString得到纯文本

  

#### 2.4 文本清理

推荐在解析时引入文本清理方法



#### 2.5 文本分块

1.ChunkMode：创建策略枚举，定义分块策略

2.VectorChunk：分块后的结果类（为向量化预备）

其实主要字段就是切割后的知识块内容与向量化信息

```java
/**
 * 分块结果类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VectorChunk {
    /**
     * 分块ID
     */
    private String id;
    /**
     * 知识块内容
     */
    private String content;
    /**
     * 知识库索引（从0开始）
     */
    private int index;
    /**
     * 块的向量嵌入表示
     * 用于向量相似度检索的浮点数数组
     */
    private float[] embedding;
    /**
     * 块的元数据信息
     */
    private String metadata;
}
```

3.ChunkConfig：分块参数配置

```java
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChunkConfig {

    /**
     * 块的目标大小（字符数）
     */
    @Builder.Default
    private Integer chunkSize = 512;

    /**
     * 相邻块之间的重叠大小
     */
    @Builder.Default
    private Integer overlapSize = 128;
}
```









4.ChunkingStrategy：定义分块器的统一分块能力

主要功能：文本切割

```java
    /**
     * 进行文本切割
     * @param text 原始文本
     * @param config 分块参数
     * @return
     */
List<VectorChunk> split(String text, ChunkConfig config);
```





5.实现固定大小分块逻辑
