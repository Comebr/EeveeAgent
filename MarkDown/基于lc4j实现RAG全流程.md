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



**参数列表：**

| 链式方法          | 对应旧参数            | 核心作用                |
| ----------------- | --------------------- | ----------------------- |
| `scoringModel()`  | -（新增核心配置）     | 指定重排序打分模型      |
| `minScore()`      | `similarityThreshold` | 过滤低相关性 Chunk      |
| `maxResults()`    | `topN`                | 限制最大返回 Chunk 数量 |
| `querySelector()` | -（查询定位）         | 提取 / 定位用户原始提问 |



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

8.会话记忆策略类采用本地缓存，易出现并发冲突导致丢失、覆盖，策略不够灵活

9.重排模型目前不支持

10.各类设计模型调用的工具类，其提示词模版都是英文



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





### 多轮对话-自动摘要



主流实现逻辑：**批量攒够 N 轮原始会话 → 把这 N 轮（用户问 + AI 答）整体浓缩成 1 条摘要 → 删除被压缩的原始会话明细 → 保留摘要 + 新增最新会话**；



#### **细节：**

1.建两个区，一个是原始会话区（存单条历史记忆），一个是历史摘要区

获取会话记忆时，从这两个中封装historyMessages

收尾的时候先去校验当前会话记忆是否达到阈值，达到了就先进行压缩得到摘要，然后存入历史摘要区，新产生的消息加入原始会话区

好处就是：压缩逻辑干净，不会搞乱已有的摘要



2.针对轮次而非消息数，因为**轮是完整语义单元，消息条数是碎片化单元**。避免缺失上下文语义



3.压缩执行的时机是：本轮对话结束后



4.异步执行，所以单独的Redis操作可以划分一个专属的线程池



5.摘要的类型要是`SYSTEM`：系统级消息，用于**上下文补充、历史摘要、规则设定**

### 实现：

1.按轮次分割，对于单独的消息进行清理，得到纯轮次列表

2.判断轮次数是否超过阈值

3.超过，对前（最大轮次-2）进行压缩

4.构建专属提示词模版

5.调用大模型生成摘要

6.存入Redis摘要缓存

7.更新原始会话记忆

#### 扩展：

阈值的类型也有很多
1.轮次阈值

2,文本长度阈值

3.token阈值

4.并发阈值.......







## 提示词工程

整体架构设计：

分层设计：整个提示词工程分为：资源管理层、渲染引擎层、编排业务层、缓存优化层这四个层面，构建了完整的提示词处理流水线。

集中管理：通过常量类统一管理提示词路径，实现提示词工程的可维护



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

**核心点：基础意图识别构建、意图识别识别流程resolve**

流程：

resolve（classifyIntents）——》capTotalIntents（collectAllCandidates、selectTopIntentPerSubQuestion、selectAdditionalIntents）—— 》rebuildSubIntents

****



前置：封装完整的用户问题重写结果

开始意图识别：

resolve：

1.校验结果是否包含子问题？子问题：精简原问题

2.采用专属线程池异步进行意图节点打分筛选：

​	得到List<NodeScore>——》封装成对象(子问题+对应节点打分结果)

3.得到子问题与其意图候选列表

capTotalIntents：

1.计算总意图数

2.若未超限，步出

3.超限：

- 步骤1：收集所有意图，按子问题索引分组，降序排序
- 步骤2：每个子问题保留最高分意图，截取第一个
- 步骤3：计算剩余配额（问题数<最大意图数就会产生剩余）
- 步骤4：从剩余候选中按分数选择
- 步骤5：合并并重建结果





rebuildSubIntents：这一步的核心就是按照问题索引去重构出最终的意图识别结果，也就是子问题字符串+意图节点

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

## 召回

检索阶段要根据



## rerank重排

### LangChain4j-Rerank

官方提供了两种实现重排的策略

1.基于RRF算法

```java
    /**
     * 基于 RRF（互惠排名融合）算法实现多源结果融合（默认实现）
     * @param rerankRequestMap
     * @return
     */
    public List<Content> contentAggregatorWithRRF(Map<Query, Collection<List<Content>>> rerankRequestMap){
        DefaultContentAggregator defaultContentAggregator = new DefaultContentAggregator();
        List<Content> aggregate = defaultContentAggregator.aggregate(rerankRequestMap);
        return aggregate;
    }

```



2.使用rerank模型进行重排

```java
/**
     * 高精度语义重排
     * @param rerankRequestMap
     * @return
     */
    public List<Content> contentAggregatorWithReRank(Map<Query, Collection<List<Content>>> rerankRequestMap){
        /**
         * 参数列表：
         * .scoringModel()    // 1. 重排序打分模型（核心）
         * .minScore()        // 2. 最小分数阈值（过滤）
         * .maxResults()      // 3. 最大返回结果数（截断）TopN
         * .querySelector()   // 4. 查询提取器（定位用户问题）
         */
        ReRankingContentAggregator reRankingContentAggregator = ReRankingContentAggregator
                .builder()
                .scoringModel()
                .minScore()
                .maxResults()
                .querySelector()
                .build();
        List<Content> aggregate = reRankingContentAggregator.aggregate(rerankRequestMap);
        return aggregate;
    }
```



LangChain4j 的核心设计理念 ——**提供抽象接口，鼓励用户根据自身需求灵活实现**。方便适应生态多样性、解耦。

![image-20260415104659794](./../../../images/image-20260415104659794.png)



根据源码可以看出，我们只需实现ScoringModel接口，并自定义继承的方法便可做适配

### DashScope-Rerank

本项目使用Qwen-Rerank来实现

#### 依赖

```xml
        <!-- 3. Dashscope社区依赖接入 -->
<dependency>
      <groupId>dev.langchain4j</groupId>
      <artifactId>langchain4j-community-dashscope-spring-bootstarter</artifactId>
</dependency>
```

2.22.6版本，百炼官方提供了如下实体

![image-20260415110309144](./../../../images/image-20260415110309144.png)

#### TextRerank：

**1.成员变量：**

**`log`**：SLF4J 日志记录器，用于记录运行时日志。

**`syncApi`**：`SynchronizeHalfDuplexApi<TextReRankParam>` 类型，负责底层的同步半双工通信（请求 - 响应模式）。

**`serviceOption`**：`ApiServiceOption` 类型，通过 `defaultApiServiceOption()` 初始化，定义了服务调用的核心配置（协议、HTTP 方法、任务类型等）。

**2. `call(TextReRankParam param)`**

核心调用方法：

- 先验证 `param` 参数合法性（`param.validate()`）。
- 强制设置为**非流式模式**（`SSE=false`，`StreamingMode.NONE`）。
- 通过 `syncApi.call(param)` 发起请求，再将原始响应转换为 `TextReRankResult` 类型返回。





#### TextRerankParam

负责封装调用 `TextReRank` 时所需的所有输入数据，并完成参数校验、请求体构建等工作。

| 字段名            | 类型           | 作用说明                                                     |
| ----------------- | -------------- | ------------------------------------------------------------ |
| `query`           | `String`       | **必填**。重排序的查询文本，最大长度 4000 tokens。           |
| `documents`       | `List<String>` | **必填**。待重排序的候选文档列表，最多 500 条。`@Singular` 注解通常用于 Builder 模式，支持逐个添加文档。 |
| `topN`            | `Integer`      | 可选。指定返回的 top-K 文档数量。若不填或超过文档总数，返回全部文档。 |
| `returnDocuments` | `Boolean`      | 可选。是否在结果中返回原始文档内容，默认为 `false`。         |
| `instruct`        | `String`       | 可选。重排序的指令文本，用于自定义重排序的逻辑导向。         |

#### **TextReRankOutput**

文本重排序服务的**响应结果类**

| 字段    | 类型                | 含义                                           |
| ------- | ------------------- | ---------------------------------------------- |
| results | `List<Result>` 类型 | 存储所有重排序后的结果列表，默认按分数降序排列 |

**内部类1.`Result`（重排序结果项）**

| 字段           | 类型     | 含义                                                         |
| -------------- | -------- | ------------------------------------------------------------ |
| index          | Interger | 原始文档在输入列表中的索引位置                               |
| relevanceScore | Double   | 相关性得分                                                   |
| document       | Document | 仅当请求参数 `returnDocuments` 设为 `true` 时，该字段才会有值，包含原始文档的内容。 |

**内部类2.Document（文档内容）**

| 字段 | 类型   | 含义                       |
| ---- | ------ | -------------------------- |
| text | String | 存储候选文档的原始文本内容 |



#### TextReRankResult（最终响应结果封装类）



| 字段名       | 类型               | 作用说明                                                     |
| ------------ | ------------------ | ------------------------------------------------------------ |
| `requestId`  | `String`           | 通过 `@SerializedName("request_id")` 映射 JSON 字段，唯一标识本次 API 请求，用于问题排查。 |
| `usage`      | `TextReRankUsage`  | 本次请求的 Token 用量统计（如输入 Token 数、总 Token 数等）。 |
| `output`     | `TextReRankOutput` | 重排序的核心业务结果，包含排序后的文档列表、相关性得分等（即上一节解析的类）。 |
| `statusCode` | `Integer`          | 通过 `@SerializedName("status_code")` 映射 JSON 字段，HTTP 状态码（如 200 表示成功）。 |
| `code`       | `String`           | 业务状态码（如 "Success" 表示成功，错误时返回具体错误码）。  |
| `message`    | `String`           | 业务状态描述（成功时通常为空，错误时返回错误详情）。         |



#### TextReRankUsage（Token 用量统计类）

| 字段名        | 类型      | 作用说明                                                     |
| ------------- | --------- | ------------------------------------------------------------ |
| `totalTokens` | `Integer` | 通过 `@SerializedName("total_tokens")` 映射 JSON 字段，表示本次请求消耗的 **总 Token 数**（包含 `query` 和所有 `documents` 的 Token 总和）。 |



#### 实战





## 多通道检索

当前流程分析：

目前只有一种检索通道：按具体的意图节点进行检索

新增



MultiChannelRetrievalEngine：

1.多通道并行检索

主要流程：

- 过滤出启用的通道
- 采用专属线程池并行执行所有通道search
- 对于出现异常的检索通道设置置信度为0
- 统计所有通道的检索结果，并打印日志

2.后置处理器链

- 过滤出启用的处理器，无那就返回原始结果
- 按排序后的顺序依次执行处理器：去重后置处理器、Rerank重排处理器
- process执行



下一步：实现具体通道策略器、实现具体后置处理器、定义对应的策略类

检索策略：意图定向检索 + 向量全局检索

本质上都是检索Collection，区别就是作用域不同，当无意图时采用全局检索

意图会存在多个的情况、全局检索更不用说，若串行检索，耗时较长，所以才引入了并行检索执行器，对应两种策略同样也有两种并行检索器，不难看出，二者的检索方法一样作用域不同，因此可以做一个模版类定义模版方法，核心方法模式就是检索的相同点

所以第一步可以先将我们目前的意图定向方法移植过来：

原始传入的si虽说是子问题+意图，但意图并为真正分类筛选，可能是KB、SYS、null，













问题重写——子问题进行意图识别——可能会存在多种意图（目前就两个：系统问题、知识库检索）

——执行检索流程（RetrievalContext）

既然是有多钟意图，那我们左检索时就要分别出来，系统意图你就去走系统检索通道，KB意图就进行KB检索通道处理，最后汇总起来，得到最终的检索结果

——根据得到的结果进行提示词模版封装（PromptContext）

——封装好的提示词喂给大模型 

——输出内容

PromptContext

——RetrievalContext

​	——ReCallContext

​	——ReRankContext

至于那些清理、格式化先不用去管，当完整流程打通之后，再进行微调

retrieve(buildSubQuestionContext)	





## 生成



整合全流程到聊天服务

1.对用户提问进行重写









## 会话管理

细节：首次发起对话并完成后创建，

前端的新对话按键要做防抖，点击之后预览新的聊天界面，并调用后端的接口获取唯一对话id，但是不调用创建窗口，等到用户发起之后再将得到的会话id传给后端创建，管他完没完成，



## 会话终止

**核心接口：StreamingHandle**

位于onPartialResponse部分反应阶段，PartialResponseContext对象携带的参数

### 实现类：

![image-20260424092055172](./../../../images/image-20260424092055172.png)

1.CancellationUnsupportedStreamingHandle作用是当非指定流式输出部分反应阶段时若调用cancel方法，会抛出异常：

- 这个 StreamingChatModel 实现类不支持「流式取消」功能；并且你调用了错误的方法，应该使用带两个参数的 onPartialResponse(响应对象, 上下文)，而不是只传字符串的 onPartialResponse(字符串)。

2.匿名内部类ServerSentEventParsingHandleUtils

这个类是一个**适配器工具类**，作用只有一个：

- 把 **SSE（服务器发送事件）专用的解析句柄 `ServerSentEventParsingHandle`** → 转换成 **LangChain4j 通用的流式控制句柄 `StreamingHandle`**。
- 你调用 `StreamingHandle.cancel()`
- 这个工具类会**立刻转发**给真正的 SSE 流句柄 `parsingHandle.cancel()`
- 底层 `parsingHandle.cancel()` 才是**真正执行停止操作**的代码（比如：关闭 HTTP 连接、停止接收 SSE 事件、终止流式解析）



#### 核心方法：

```java
StreamingHandle streamingHandle = context.streamingHandle();
streamingHandle.cancel();
```













## RAGent项目详细RAG流程
### 1. 用户提问阶段
- 输入 ：用户原始问题
- 处理 ：接收用户输入，生成会话ID和任务ID
### 2. 记忆加载阶段
- 组件 ： ConversationMemoryService
- 功能 ：加载历史对话记录，支持多轮对话上下文
- 代码 ： memoryService.loadAndAppend(conversationId, userId, ChatMessage.user(question))
### 3. 查询改写阶段
- 组件 ： QueryRewriteService
- 功能 ：
  - 将自然语言问题改写为适合检索的简洁查询
  - 支持多问句拆分
  - 结合对话历史进行指代消解
- 代码 ： queryRewriteService.rewriteWithSplit(question, history)
- 输出 ： RewriteResult 包含改写后的问题和子问题列表
### 4. 意图识别阶段
- 组件 ： IntentResolver
- 功能 ：
  - 对每个子问题进行意图分类
  - 并发处理多个子问题
  - 过滤低分意图（分数 < 0.35）
  - 限制最大意图数量（最多3个）
- 代码 ： intentResolver.resolve(rewriteResult)
- 输出 ： List<SubQuestionIntent> 包含每个子问题及其相关意图
### 5. 歧义引导阶段
- 组件 ： IntentGuidanceService
- 功能 ：
  - 检测用户问题是否存在歧义
  - 当意图识别结果包含多个相似选项时，生成引导式问答
  - 提示用户明确具体需求
- 代码 ： guidanceService.detectAmbiguity(rewriteResult.rewrittenQuestion(), subIntents)
- 输出 ： GuidanceDecision 决定是否需要引导用户
### 6. 系统意图判断阶段
- 组件 ： IntentResolver
- 功能 ：
  - 判断是否所有意图都是系统意图（SYSTEM类型）
  - 如果是系统意图，使用自定义提示词直接回答
  - 跳过检索流程
- 代码 ： intentResolver.isSystemOnly(si.nodeScores())
### 7. 检索阶段
- 组件 ： RetrievalEngine
- 功能 ：
  - MCP工具调用 ：根据MCP意图调用相应工具获取动态数据
  - 多通道检索 ：支持意图导向检索和全局检索
  - 并发检索 ：多个检索通道并发执行
  - 后处理 ：去重、重排序等 7.1 MCP工具调用
- 组件 ： MCPToolExecutor
- 功能 ：
  - 从用户问题中提取MCP工具参数
  - 调用相应的MCP工具（如数据库查询、API调用等）
  - 获取动态数据作为MCP上下文 7.2 多通道检索
- 组件 ： MultiChannelRetrievalEngine
- 检索通道 ：
  - 意图导向检索 ：根据意图节点定向检索特定知识库
  - 全局检索 ：在所有知识库中进行全局搜索
- 并发策略 ：多个检索通道并发执行，提高检索效率 7.3 后处理
- 去重 ： DeduplicationPostProcessor 去除重复的检索结果
- 重排序 ： RerankPostProcessor 使用Rerank模型对结果进行重新排序
- 代码 ： retrievalEngine.retrieve(subIntents, DEFAULT_TOP_K)
- 输出 ： RetrievalContext 包含MCP上下文、知识库上下文和分组的检索块
### 8. 提示词组装阶段
- 组件 ： RAGPromptService
- 功能 ：
  - 根据场景选择合适的提示词模板（KB_ONLY、MCP_ONLY、MIXED）
  - 组装结构化消息序列
  - 处理多子问题的显式编号
  - 添加历史对话记录
- 代码 ： promptBuilder.buildStructuredMessages(context, history, question, subQuestions)
- 输出 ： List<ChatMessage> 完整的消息序列
### 9. 流式输出阶段
- 组件 ： LLMService
- 功能 ：
  - 调用大模型生成回答
  - 支持流式输出
  - 支持任务取消
- 代码 ： llmService.streamChat(request, callback)
- 输出 ：通过SSE向客户端流式返回回答







当前执行链路：

reslove——》classifyIntents——》DefaultIntentClassifier.classifyTargets——》DefaultIntentClassifier.buildIntentPromptTemplate——》PromptTemplateLoader.render——》