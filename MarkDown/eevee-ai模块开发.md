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

### 1.知识库

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



##### service接口

文档业务所需的功能列表

| 接口名 | 参数 | 功能描述 |
| :----: | :--: | :------: |
|        |      |          |
|        |      |          |
|        |      |          |























#### 03.构建知识块表

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













#### 05.文件上传实现

于文档模块部分实现























| 接口名           | UserUpdateService                                            |                                                              |
| ---------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **接口请求地址** | [www.baidu.com](https://link.juejin.cn/?target=http%3A%2F%2Fwww.baidu.com) |                                                              |
| **功能说明**     | UserUpdateService接口是应用系统的账号修改方法                |                                                              |
| **请求参数**     | **参数名**                                                   | **中文说明**                                                 |
|                  | RequestId                                                    | 平台每次调用生成的随机ID，应用系统每次响应返回此ID，String类型 |
|                  | uid                                                          | 三方应用系统账号创建时，返回给应用系统的账号主键uid。**必传字段** |
|                  | loginName/ fullName                                          | 需要修改的账号字段属性                                       |
| **响应参数**     | **参数名**                                                   | **中文说明**                                                 |
|                  | RequestId                                                    | 平台每次调用接口发送的请求ID，字段为String类型               |
|                  | resultCode                                                   | 接口调用处理的结果码，**0为正常处理**，其它值由应用系统定义。字段为String类型，**必传字段**。 |
|                  | message                                                      | 接口调用处理的信息。字段为String类型                         |
| **请求示例：**   | { “token”,””, “treeCode”,” EXECUTIVE”, “code”,””}            | markdown展示不是很好看，建议word                             |
| **返回值**       | { "xxxx": "xxxxxx", "resultCode": "0", "message": "success" } | markdown展示不是很好看，建议word                             |

