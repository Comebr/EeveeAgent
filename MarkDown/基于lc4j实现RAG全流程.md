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



> 有的同学看到这，会很悲观，langchain4j缺点这么多，还有必要学吗QAQ？答案是：一定要学，不仅要学langchain4j，还要学SpringAI、SAA...不管任何技术都有一定的缺陷，没有完美的技术，只有懒惰的人。langchain4j的弱势项可以用其他框架的优点去弥补。凡事都要循序渐进，没有谁一出生就是架构师，通过各类框架的学习增长自身的工程能力、架构能力。
>







## 会话记忆



### 1。ChatMemory接口

> 表示聊天对话的内存（历史）。由于语言模型不保留对话状态，因此必须在每次与语言模型交互时提供所有之前的信息。{@link ChatMemory} 帮助跟踪对话，确保消息符合语言模型的上下文窗口。

参数：id 表示聊天记忆的实体类id

| 方法名                               | 作用                                                         |
| ------------------------------------ | ------------------------------------------------------------ |
| add (ChatMessage... messages)        | 将消息添加到聊天内存中                                       |
| set (Iterable<ChatMessage> messages) | 将聊天内存中的所有消息替换为指定的消息。该方法替换整个消息历史，而不是附加到历史上。这种方法通常用于需要重写聊天内存以实现内存压缩等功能 |
| messages                             | 从聊天内存中提取消息。根据实现方式不同，它可能不会返回所有先前添加的消息，而是返回子集、摘要或它们的组合。 |
| clear                                | 清除聊天记忆                                                 |



![image-20260412163822056](./../../../images/image-20260412163822056-1775983324347-1.png)



#### MessageWindowChatMemory消息条数窗口记忆

**作用：**以**消息条数**为窗口边界，自动维护最近 N 条消息，超出则淘汰最旧消息，确保上下文不无限增长。



**实现原理：**

1.核心参数：`maxMessages`（默认值 10），控制窗口保留的最大消息数

2.数据结构：内部维护有序消息列表，按时间戳升序排列

3.窗口机制：

- 新消息加入时，先追加至列表尾部
- 检查总条数是否超过`maxMessages`
- 若超出，从列表头部移除最早消息，直至条数符合限制

4.特殊处理：SystemMessage 默认不会被淘汰，始终保留在窗口中



**适用场景：**快速开发





#### TokenWindowChatMemory精准 Token 数量窗口记忆

**作用：**以**Token 总数**为窗口边界，确保对话历史总 Token 数不超过模型上下文限制，同时优化 Token 成本。

**实现原理：**

核心参数：

- `maxTokens`：窗口允许的最大 Token 数（如 4000）
- `tokenCountEstimator`：Token 计数器，用于估算每条消息的 Token 数

核心算法（1.11.0）：

1. 新消息加入前，先计算当前窗口总 Token 数
2. 估算新消息 Token 数，预判是否超出`maxTokens`
3. 若超出，从列表头部开始移除最早消息，直至总 Token 数≤`maxTokens`
4. 追加新消息至列表尾部

特殊处理：

- 优先保留 SystemMessage，仅在必要时才淘汰
- 消息作为整体处理，不会拆分单条消息的 Token

**适用场景：**RAG

### 2.ChatMemoryStore持久化

![image-20260412165622058](./../../../images/image-20260412165622058.png)



#### InMemoryChatMemoryStore

**作用：**基于 **JVM 内存** 的临时对话存储，是 LC4J **默认、零依赖**的记忆存储实现，



**实现原理：**

- 底层用 `ConcurrentHashMap<String, List<ChatMessage>>` 存储
- Key：`memoryId`（用户 / 会话唯一标识）
- Value：该会话的完整对话消息列表
- 线程安全、无第三方依赖、读写速度极快
- **服务重启 / 容器销毁 → 所有记忆彻底丢失**



**适用场景：**临时会话





#### SingleSlotChatMemoryStore











## 提示词工程

整体架构设计：

分层设计：整个提示词工程分为：资源管理层、渲染引擎层、编排业务层、缓存优化层这四个层面，构建了完整的提示词处理流水线。

集中管理：通过常量类统一管理提示词路径，实现提示词工程的可维护性



具体内容：

资源管理层

### 资源管理层

- PromptTemplateLoader ：负责从类路径加载提示词模板，支持缓存机制
- 采用 ConcurrentHashMap 实现进程内缓存，提升性能
- 支持模板变量填充，实现动态内容注入

##### PromptTemplateLoader

```java
/**
 * 提示词模版加载器
 */
@Slf4j
@RequiredArgsConstructor
public class PromptTemplateLoader {

    private final ResourceLoader resourceLoader;
    private final Map<String, String> cache = new ConcurrentHashMap<>();


    /**
     * 加载指定路径的提示模板
     *
     * @param path 模板文件路径，支持classpath:前缀
     * @return 模板内容字符串
     * @throws IllegalArgumentException 当路径为空时抛出
     * @throws IllegalStateException    当模板文件不存在或读取失败时抛出
     */
    public String load(String path) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("提示模板路径为空");
        }
        // 本地缓存进ConcurrentHashMap
        return cache.computeIfAbsent(path, this::readResource);
    }

    /**
     * 渲染提示模板，将模板中的占位符替换为实际值
     *
     * @param path  模板文件路径
     * @param slots 占位符映射表，键为占位符名称，值为替换内容
     * @return 渲染后的完整提示文本
     */
    public String render(String path, Map<String, String> slots) {
        String template = load(path);
        String filled = PromptTemplateUtils.fillSlots(template, slots);
        return PromptTemplateUtils.cleanupPrompt(filled);
    }



    /**
     * 从资源路径中读取模版文件
     */
    public String readResource(String path){
        //归一化路径为根目录
        String location = path.startsWith("classpath:") ? path : "classpath:" + path;

        //使用resourceLoader加载
        Resource resource = resourceLoader.getResource(location);
        if(!resource.exists()){
            throw new IllegalStateException("提示词模版路径不存在：" + path);
        }
        //读取文件的输入流
        try (InputStream inputStream = resource.getInputStream()){
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }catch (IOException e){
            log.error("读取提示模板失败，路径：{}", path, e);
            throw new IllegalStateException("读取提示模板失败，路径：" + path, e);
        }
    }
}
```







### 渲染引擎层

> 构建提示词工程的 渲染引擎层 ，扩展模板渲染能力，支持更复杂的变量类型和渲染场景。这一层是连接资源管理层和编排服务层的桥梁，提供灵活、强大的模板渲染功能。



AdvancedPromptRenderer

PromptContext

### 编排服务层



### 缓存优化层













































## 意图识别

#### 1.数据表设计

核心：子点标识、所属父节点标识、结点类型、collection_name、top_k

```sql
CREATE TABLE `intent_node` (
  `id` bigint NOT NULL COMMENT '节点自增主键',
  `kb_id` bigint DEFAULT NULL COMMENT '关联知识库id',
  `intent_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '意图节点唯一标识',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点名称',
  `level` tinyint NOT NULL COMMENT '层级{0：DOMAIN 1：CATEGORY 2：INTENT}\r\n 注意：层级不建议过多\r\n 领域——》分类/场景——》意图',
  `parent_code` varchar(65) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父节点唯一标识，默认null：ROOT',
  `description` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点描述',
  `examples` text COLLATE utf8mb4_unicode_ci COMMENT '示例问题',
  `kind` tinyint(1) NOT NULL COMMENT '类型：0：RAG知识库类 1：System系统交互类',
  `collection_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联的Collection表名称',
  `top_k` int DEFAULT NULL COMMENT '知识库检索粗筛TopK，null：全局统一TopK',
  `prompt_snippet` text COLLATE utf8mb4_unicode_ci COMMENT '提示词片段',
  `prompt_template` text COLLATE utf8mb4_unicode_ci COMMENT '提示词模版',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序字段',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用 1：启用 0：禁用',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `del_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0：正常 1：删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG意图树节点配置表';
```





#### 2.枚举类设计

可扩展，意图层级不建议超过3层

IntentLevel.java ：意图层级枚举

```java
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azheng.boot.intention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 意图层级枚举
 * 用于表示知识库中意图的层级
 */
@Getter
@RequiredArgsConstructor
public enum IntentLevel {

    /**
     * 顶层：集团信息化 / 业务系统 / 中间件环境
     */
    DOMAIN(0),

    /**
     * 第二层：人事 / 行政 / OA系统 / Redis ...
     */
    CATEGORY(1),

    /**
     * 第三层：更具体的 INTENT，如 系统介绍 / 数据安全 / 架构设计
     */
    INTENT(2);

    private final int code;

    /**
     * 根据编码获取对应的意图层级
     *
     * @param code 层级编码
     * @return 对应的IntentLevel枚举值，如果code为null或不存在则返回null
     */
    public static IntentLevel fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IntentLevel e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    /**
     * 返回枚举的名称
     *
     * @return 枚举名称字符
     */
    @Override
    public String toString() {
        return name();
    }
}



```

IntentKind.java ：意图类型枚举

```java
package com.azheng.boot.intention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 意图类型枚举
 * 用于区分用户意图的不同类型
 */
@Getter
@RequiredArgsConstructor
public enum IntentKind {

    /**
     * 知识库类，走 RAG
     */
    KB(0),

    /**
     * 系统交互类，比如欢迎语、介绍自己
     */
    SYSTEM(1);


    /**
     * 意图类型编码
     */
    private final int code;

    /**
     * 根据编码获取对应的意图类型
     *
     * @param code 意图类型编码
     * @return 对应的意图类型枚举值，如果编码不存在则返回null
     */
    public static IntentKind fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IntentKind e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    /**
     * 返回枚举名称
     *
     * @return 枚举的名称字符串
     */
    @Override
    public String toString() {
        return name();
    }
}


```

#### 3.数据模型类









## 多路召回



## rerank重排





## 生成