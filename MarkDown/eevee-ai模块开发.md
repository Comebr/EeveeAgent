# eevee-ai模块开发



> 项目初期：基于langchain4j实现全流程,后期引入SpringAI、SAA来重构
>
> 注意：eevee-ai层不写启动类、配置文件，以及HTTP接口暴露Controller，交给main-application实现

#### 1.引入相关依赖

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





#### 2.模型选择

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





#### 3.实现简单的流式输出聊天对话

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

