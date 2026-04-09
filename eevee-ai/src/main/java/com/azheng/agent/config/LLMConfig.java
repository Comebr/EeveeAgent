package com.azheng.agent.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {

    /**
     * 流式输出模型-百炼
     */
    @Bean(name = "qwen-flash")
    public StreamingChatModel QwenFlashStreamingChatModel(){
        return OpenAiStreamingChatModel
                .builder()
                .apiKey(System.getenv("BAILIAN_API_KEY"))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .modelName("qwen-flash")
                .build();
    }





    /**
     * 向量模型-百炼
     */
    @Bean(name = "embedding-v4")
    public EmbeddingModel defaultEmbeddingModel(){
        return OpenAiEmbeddingModel
                .builder()
                .modelName("text-embedding-v4")
                .dimensions(4096)
                .build();
    }
}
