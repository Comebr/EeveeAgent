package com.azheng.boot.rag.config;

import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import com.azheng.boot.rag.memory.RedisChatMemoryStore;
import com.azheng.boot.rag.service.RAGChatService;
import dev.langchain4j.community.model.dashscope.QwenTokenCountEstimator;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

@Configuration
public class LLMConfig {

    @Value(value = "${langchain4j.community.dashscope.base-url}")
    private String baseUrl;

    @Value(value = "${langchain4j.community.dashscope.api-key}")
    private String apiKey;

    @Value(value = "${langchain4j.community.dashscope.dimensions}")
    private Integer dimensions;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 流式输出模型-百炼
     * 职责：前端会话输出
     */
    @Bean(name = "qwen-flash")
    public StreamingChatModel QwenFlashStreamingChatModel(){
        return OpenAiStreamingChatModel
                .builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("qwen-flash")
                .temperature(0.7)
                .topP(0.9) //控制词的多样性
                .maxTokens(2048) //单次生成最大Token
                .frequencyPenalty(0.1) //降低重复词频率
                .presencePenalty(0.1) //避免重复生成相同内容
                .build();
    }

    /**
     * 职责：后台脏活累活
     */
    @Bean(name = "qwen-turbo")
    public ChatModel retrievalToolModel(){
        return OpenAiChatModel
                .builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("qwen-turbo")
                .temperature(0.1)
                .topP(0.1)
                .maxTokens(150)
                .frequencyPenalty(0.2)   // 降低重复词频率
                .presencePenalty(0.2)    // 避免重复生成相同内容
                .build();
    }





    /**
     * 向量模型-百炼
     * 该模型最大向量维度为 3072
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


    @Bean
    public ChatMemoryProvider chatMemoryProvider(){
        // 使用 gpt-3.5-turbo 作为模型名称来估算 token 数量，因为 qwen-flash 使用类似的 token 编码
        TokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator("gpt-3.5-turbo");
        return memoryId -> TokenWindowChatMemory
                .builder()
                .id(memoryId)
                .maxTokens(1000,tokenCountEstimator)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }
}
