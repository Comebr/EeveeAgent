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