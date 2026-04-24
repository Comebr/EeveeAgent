package com.azheng.boot.rag.core.memory;


import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 针对于原始会话
 * 达到轮次上限自动触发压缩摘要
 */
@Service
public class HistoryAbstractStore {

    private static final String ABSTRACT_KEY_PREFIX = "EeveeAgent:chat:memory:abstract:";
    private static final String SESSION_KEY_PREFIX = "EeveeAgent:chat:memory:session:";

    // 摘要过期时间：30天
    private static final Duration EXPIRE_TIME = Duration.ofDays(30);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 新增
     */
    public void insert(Object conversationId, List<ChatMessage> messages) {
        String key = buildKey(conversationId);
        String value = ChatMessageSerializer.messagesToJson(messages);
        stringRedisTemplate.opsForValue().set(key, value, EXPIRE_TIME);
    }

    /**
     * 删除
     */
    public void delete(Object conversationId) {
        String key = buildKey(conversationId);
        stringRedisTemplate.delete(key);
    }

    /**
     * 修改
     */
    public void update(Object conversationId, List<ChatMessage> messages) {
        String key = buildKey(conversationId);
        String value = ChatMessageSerializer.messagesToJson(messages);
        stringRedisTemplate.opsForValue().set(key, value, EXPIRE_TIME);
    }

    /**
     * 查询
     */
    public List<ChatMessage> query(Object memoryId) {
        String key = buildKey(memoryId);
        String messages  = stringRedisTemplate.opsForValue().get(key);
        if (messages == null) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(messages);
    }

    /**
     * 更新被压缩的原始会话
     */
    public void updateSession(Object conversationId, List<ChatMessage> messages) {
        String key = buildKeyForSession(conversationId);
        String value = ChatMessageSerializer.messagesToJson(messages);
        stringRedisTemplate.opsForValue().set(key, value, EXPIRE_TIME);
    }



    /**
     * 构造Key
     */
    public String buildKey(Object memoryId) {
        return ABSTRACT_KEY_PREFIX+memoryId.toString();
    }

    /**
     * 构造Key
     */
    public String buildKeyForSession(Object memoryId) {
        return SESSION_KEY_PREFIX+memoryId.toString();
    }
}
