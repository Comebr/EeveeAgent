package com.azheng.agent.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {

    @Value(value = "${langchain4j.open-ai.base-url}")
    private String baseUrl;

    @Value(value = "${langchain4j.open-ai.api-key}")
    private String apiKey;

    @Value(value = "${langchain4j.open-ai.embedding-model.dimensions}")
    private Integer dimensions;

    /**
     * 流式输出模型-百炼
     * 该模型最大向量维度为 3072
     */
    @Bean(name = "qwen-flash")
    public StreamingChatModel QwenFlashStreamingChatModel(){
        return OpenAiStreamingChatModel
                .builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
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
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("text-embedding-v4")
                .dimensions(dimensions)
                .build();
    }
}
