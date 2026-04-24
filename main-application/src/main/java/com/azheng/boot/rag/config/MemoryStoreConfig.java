package com.azheng.boot.rag.config;

import com.azheng.boot.rag.core.memory.ConcurrentRedisChatMemory;
import com.azheng.boot.rag.core.memory.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MemoryStoreConfig {

    private final RedisChatMemoryStore redisChatMemoryStore;


    /**
     * 纯净会话记忆器
     * 不采用官方提供的会话记忆摘要实现类，通过专属多轮会话
     * @return
     */
    @Bean
    public ChatMemoryProvider originalChatMemoryProvider(){
        return memoryId -> new ConcurrentRedisChatMemory(
                memoryId,
                redisChatMemoryStore
        );
    }

    @Bean
    public ChatMemoryProvider tokenWindowChatMemoryProvider(){
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
