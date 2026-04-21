package com.azheng.boot.rag.core.memory;


import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class RedisChatMemoryStore implements ChatMemoryStore {
    // Redis Key 前缀（区分业务）
    private static final String KEY_PREFIX = "EeveeAgent:chat:memory:";

    // 会话过期时间：7天
    private static final Duration EXPIRE_TIME = Duration.ofDays(15);

    @Resource
    public StringRedisTemplate stringRedisTemplate;

    /**
     * 获取会话记忆缓存
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = buildKey(memoryId);
        String messages  = stringRedisTemplate.opsForValue().get(key);
        return ChatMessageDeserializer.messagesFromJson(messages);
    }

    /**
     * 更新：不存在就插入，存在就更新
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = buildKey(memoryId);
        // 将 List<ChatMessage> 序列化为 JSON 字符串
        String messagesJson = ChatMessageSerializer.messagesToJson(messages);
        stringRedisTemplate.opsForValue().set(key, messagesJson, EXPIRE_TIME);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = buildKey(memoryId);
        stringRedisTemplate.delete(key);
    }

    /**
     * 构造Key
     */
    public String buildKey(Object memoryId) {
        return KEY_PREFIX+memoryId.toString();
    }
}
