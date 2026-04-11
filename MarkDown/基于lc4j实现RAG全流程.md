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

注：官方嵌入集成不好用@-@

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





## 检索阶段：

lc4j提供了5个核心组件：

- `QueryTransformer`
- `QueryRouter`
- `ContentRetriever`
- `ContentAggregator`
- `ContentInjector`



![img](./../../../images/advanced-rag-fb84283d02470b835ff2f4913f08fdbf.png)



**流程：**

1.用户产生一个提问 question，会转化为UserMessage对象

2.将UserMessage转换为一个或多个QueryTransformer对象

3.每个QueryTransformer会被转化为一个或多个QueryRouter对象

4.这时，运用ContentRetriever对象，对得到的QueryRouter检索出一个或多个匹配的Chunk

5.运用ContentAggregator对象，将得到的Chunk做成一个排名列表

6.运用ContentInjector拼接提示词

7.将提示词喂给大模型

### 1.QueryTransformer接口（优化用户提问）

**核心契约：**由原始 Query衍生出一个或多个 Query的集合。官方提供了三种实现类

![image-20260411111707834](./../../../images/image-20260411111707834.png)

#### DefaultQueryTransformer（默认查询转换器）

**作用：**：**无扩展 / 纯传递**，不修改原始查询，仅将其包装为单元素集合返回



#### CompressingQueryTransformer（压缩查询转换器）

**作用：**上下文压缩扩展，结合聊天记忆将原始查询压缩为含义自转的查询

**举个例子：**

- 用户提问1：我有一个好朋友“豆豆”，他是一条田园犬
- 用户提问2："它爱吃什么?"——》扩展：“田园犬豆豆爱吃什么”

**适用场景：**对话上下文的后续查询，避免指代不清，上下文缺失问题



#### ExpandingQueryTransformer（扩展查询转换器）

**作用：**多角度扩展，使用LLM对用户提问按不同角度生成多种问法，从而扩大检索的覆盖范围

**实现原理：**

- 通过提示词Prompt去引导LLM从同义词替换、句式调整、发现潜在需求等维度重写原始question
- 生成固定数量的扩展问题，并与原始问题一起用于查询

**适用场景：**复杂场景





### 2.QueryRouter接口（意图识别+检索器路由分配）

**核心契约：**负责将用户查询动态分配到一个或多个最合适的**ContentRetriever**（内容检索器）,官方提供了两个实现类

![image-20260411141357002](./../../../images/image-20260411141357002.png)



####  DefaultQueryRouter（默认查询路由器）

**作用：**不做任何过滤与选择，全量广播



#### LanguageModelQueryRouter（LLM 驱动查询路由器）

**作用：**运用LLM分析问题的意图，去匹配预设置的上层检索器，再去选择最匹配的检索器

> **用户的问题往往是跨领域、多维度的**

**实现原理：**

1.定义检索器与对应描述的集合

2.意图分析：LLM根据query和**每一个检索器的描述做匹配度打分**

3.路由决策：只要匹配度达标，全部返回，可以返回一个或多个匹配的检索器（单路由/多路由）

**适用场景：**大规模检索，需调用大模型



#### 自定义 QueryRouter







### 3.ContentRetriever接口（查Chunk）

**核心契约：**路由选中它之后，它就去数据源里找和用户问题最相关的文本片段。

![image-20260411141427857](./../../../images/image-20260411141427857.png)



#### EmbeddingStoreContentRetriever向量检索器

**作用：**对接**向量数据库**（Chroma、Milvus、Pinecone、Elasticsearch 等），基于**语义相似度**检索内容

**原理：**

1.用户提问—>向量化

2.去向量数据库匹配最相似的Chunk

3.按相似度分数降序返回

**适用场景：**私域RAG



#### WebSearchContentRetriever 联网实时检索器

**作用：**从**互联网公开信息**中获取最新、最实时的相关内容。

**实现原理：**

1.调用第三方联网搜索API

2.根据用户question执行全网搜索，抓取网页摘要、标题等信息

3.对搜索结果进行简单排序和过滤，返回相关网页内容。

**适用场景：**联网搜索公共聊天模型服务





#### ListeningContentRetriever装饰型辅助检索器

**作用：**本身不执行实际检索，仅用于**监听、监控、增强**其他检索器的执行过程

**实现原理：**

1.对一个实际检索器（如EmbeddingStoreContentRetriever）进行包装

2.提供检索前 / 检索后的回调接口，可添加日志、统计、耗时监控等逻辑；

3.不对实际检索结果作任何影响

**适用场景：**检索日志打印、性能监控、调用统计、调试追踪、业务埋点，仅做增强，不负责核心检索。





### 4.ContentAggregator接口（聚合重排）

**核心契约：**负责将多个`ContentRetriever`（如向量检索、联网检索）返回的多份独立结果列表，进行合并、去重、重排，最终输出一份统一、高相关性的内容列表，作为 LLM 生成答案的上下文。

![image-20260411141450914](./../../../images/image-20260411141450914.png)



#### DefaultContentAggregator

**作用：**轻量无依赖的通用聚合器**，基于 RRF（互惠排名融合）算法实现多源结果融合，同时完成去重，是 LangChain4j 的默认开箱即用方案。**

**实现原理：**

1.接收多个检索器返回的、各自按相关性排序的结果列表；

2.对每个列表的每个Chunk，用RRF公式计算它的RRF总分数

**RRF (Reciprocal Rank Fusion互惠排名融合)**

**计算公式：score = Σ（1/（rank_i+k））**

> 注：k为平滑系数，默认60，Σ：求和
>
> 比如在列表 1 排第 1，列表 2 排第 3，列表 3 没出现
>
> score = （ 1/1+60）+（1/3+60）+ 0 = 0.0323

3.去重得到所有独立Chunk的得分

4.按降序排序生成最终Chunk列表



**适用场景：性能优先的生产环境（无额外模型调用，低延迟），不需要高精度重排**





#### ReRankingContentAggregator高精度语义重排聚合器

**作用：**通过专业重排大模型对候选内容做深度语义匹配，大幅提升检索结果的相关性，解决向量检索的语义偏差问题。

**实现原理：**

1.先将多个检索器的结果合并为一个候选内容池；

2.调用重排模型，输入用户提问+每个候选内容，计算每个内容与提问的语义相关性分数

3.按相关性分数降序重排候选内容，并过滤去噪等处理

4.输出Top-N最相关结果

**适用场景：**高精度RAG、模型成本换精度





### 5.ContentInjector接口（拼接提示词）

**核心契约：**负责将**聚合完成的 Chunk 内容**，按固定格式注入到提示词（Prompt）中，把「检索上下文 + 用户问题」组装成 LLM 能理解的指令，是检索环节到生成环节的**最后桥梁**。



#### DefaultContentInjector

**作用：**提供默认的提示词拼接规则，自动格式化检索到的 Chunk

**实现原理：**

1.使用内置的模版，将聚合后的所有Chunk包裹成【参考上下文】，如下源码参考

```java
    protected String format(List<Content> contents) {
        return contents.stream().map(this::format).collect(joining("\n\n"));
    }

    protected String format(Content content) {

        TextSegment segment = content.textSegment();

        if (metadataKeysToInclude.isEmpty()) {
            return segment.text();
        }

        String segmentContent = segment.text();
        String segmentMetadata = format(segment.metadata());

        return format(segmentContent, segmentMetadata);
    }
```

2.构造UserMessage

```java
        Prompt prompt = createPrompt(chatMessage, contents);
        if (chatMessage instanceof UserMessage userMessage) {
            return userMessage.toBuilder()
                    .contents(List.of(TextContent.from(prompt.text())))
                    .build();
        } else {
            return prompt.toUserMessage();
        }
```

3.拼接模版

```java
    protected Prompt createPrompt(ChatMessage chatMessage, List<Content> contents) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", ((UserMessage) chatMessage).singleText());
        variables.put("contents", format(contents));
        return promptTemplate.apply(variables);
    }
```



**适用场景：**无特殊提示词格式要求的生产级 RAG 系统。





### 6.并行化

并行：当只有一个用户提问和一个检索器时，默认使用一个线程来去执行，否则，多个情况时会使用默认线程并行处理，，当然也可以客制化线程池，





如上可以看出langchain4j框架带来的局限性：

1.嵌入客户端集成差

2.自定义提示词模版拼接不兼容，需自定义

3.仅支持**文本 + 图像**双模态基础能力，对**视频、音频、3D 模型**等复杂模态处理能力缺失

4.无内置**配置中心**（Nacos/Apollo）、**服务注册发现**、**分布式事务**支持

5.文本分割器、提示词模板、语义匹配等核心组件**默认针对英文优化**，中文语境下效果下降

6.最低要求**Java 17+**，无法兼容大量企业仍在使用的 Java 8/11 环境

7.LanguageModelQueryRouter靠一段自然语言描述让 LLM 去 “猜” 该用哪个 Retriever。描述写差一点、问题偏一点，直接路由错。完全不可控，出了问题没法排查。



有的同学看到这，会很悲观，langchain4j缺点这么多，还有必要学吗QAQ？答案是：一定要学，不仅要学langchain4j，还要学SpringAI、SAA...不管任何技术都有一定的缺陷，没有完美的技术，只有懒惰的人。langchain4j的弱势项可以用其他框架的优点去弥补。凡事都要循序渐进，没有谁一出生就是架构师，通过各类框架的学习增长自身的工程能力、架构能力。







## 会话记忆





## 意图管理
