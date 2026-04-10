# 基于lc4j实现RAG全流程

项目以具备了基础脚手架：文件上传、保存至RustFS

接下来实现索引阶段

## index stage

索引阶段流程：![img](D:/images/rag-ingestion-9b548e907df1c3c8948643795a981b95-1775618425095-3.png)



### 文档解析

实现url链接上传文档

首先前端用户操作：

1.选择上传方式：URL

2.调用后端接口直接解析url

kb目录就做基础的CRUD+文件上传+分块触发按钮调用

rag目录里面实现核心的：解析、预处理、分块、



用户点击开始分块

传参：docId、chunkMode、对应mode的参数设置（给默认值）必备字段：maxSegmentSizeInTokens、maxOverlapSizeInTokens



**文档分割家族体系**

DocumentSplitter（接口） 

├─ HierarchicalDocumentSplitter（复合递归实现）

│  └─ 内部组合：DocumentByParagraphSplitter、DocumentByLineSplitter、                                      DocumentBySentenceSplitter、DocumentByWordSplitter、DocumentByCharacterSplitter

├─ DocumentByParagraphSplitter（按段落） 

├─ DocumentByLineSplitter（按行）

├─ DocumentBySentenceSplitter（按句子） 

├─ DocumentByWordSplitter（按单词）

├─ DocumentByCharacterSplitter（按字符） 

├─ DocumentByTokenSplitter（按Token） 

├─ DocumentByRegexSplitter（按正则表达式） 

└─ 其他自定义实现...



核心参数 maxSize、maxOverlapSize





调用split方法返回List<TextSegment>

#### TextSegment

```java
// 核心字段 1：分块的文本内容    

private final String text;     

// 核心字段 2：分块的元数据（继承自 Document + 分割器自动添加）    

private final Metadata metadata;

    // 常用方法：获取文本长度
    public int textLength() {
        return text.length();
    }
```

每个 `TextSegment` 的 `metadata` 会**自动继承**原 `Document` 的所有元数据，并**额外添加**以下分块专属元数据：（可以持久化到数据库，便于CRUD）

|   元数据键    |  类型  |                         说明                         |       示例值        |
| :-----------: | :----: | :--------------------------------------------------: | :-----------------: |
|    `index`    | String |         分块在当前文档中的序号（从 0 开始）          | `"0"`、`"1"`、`"2"` |
|    `start`    | String |         分块在原文档文本中的**起始字符位置**         |   `"0"`、`"500"`    |
|     `end`     | String |         分块在原文档文本中的**结束字符位置**         |  `"499"`、`"999"`   |
| `token_count` | String | （仅 `DocumentByTokenSplitter` 有）分块的 Token 数量 |       `"300"`       |





### 文档分块





### 文本向量化存储

引入LangChain4j-Milvus集成



```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-milvus</artifactId>
    <version>1.12.2-beta22</version>
</dependency>
```





**lc4j-Milvus客户端配置**

注意：一定要提前创建好collection，若无指定名称的collection，连接时会报错

```java
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusServiceClient {


    @Bean
    public MilvusEmbeddingStore createClient(){
        return MilvusEmbeddingStore
                .builder()
                .host("192.168.191.128")
                .port(19530)
                .collectionName("milvus-embedding-EeveeAgent") //相当于数据库名称
                .dimension(4096) //向量维度，与EmbeddingModel保持一致
                .indexType(IndexType.IVF_FLAT) // 索引类型：小数据量用 FLAT，大数据量用 IVF_FLAT/HNSW
                .metricType(MetricType.COSINE) // 相似度度量：文本检索推荐用 COSINE
                .consistencyLevel(ConsistencyLevelEnum.EVENTUALLY) // 一致性级别：最终一致性性能最好
                .autoFlushOnInsert(true)      // 插入后自动刷盘（开发环境开，生产环境可关）
                .build();
    }
}
```





**MilvusEmbeddingStore**解析

> 它是 `langchain4j-milvus` 依赖中唯一的公共类，**严格实现了 LangChain4j 定义的 `EmbeddingStore<TextSegment>` 接口**，仅此而已。它的存在只有一个目的：**让LangChain4j 的 RAG 组件（如 `EmbeddingStoreContentRetriever`）无需关心底层向量数据库的差异，用统一的 API 操作向量**。



1. **自动创建连接**：根据你传入的 host/port 或已有 `MilvusServiceClient` 建立连接
2. **自动创建集合**：如果指定的集合不存在，会根据你配置的 Schema 自动创建
3. **向量插入**：将 `(向量 + TextSegment)` 批量或单条插入 Milvus
4. **向量相似度检索**：根据输入向量返回最相似的 TopK 个 `TextSegment`
5. **简单删除**：根据 ID 删除单条或多条向量（LangChain4j 0.34.0+ 才支持）



- **所以一个成熟的项目就必须单独配置官方的 `MilvusServiceClient`**



