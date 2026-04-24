package com.azheng.boot.rag.core.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 采用自定义会话记忆类
 * 避免因本地缓存导致的数据不一致、数据丢失
 */
public class ConcurrentRedisChatMemory implements ChatMemory {
    private final Object memoryId;
    private final ChatMemoryStore chatMemoryStore;

    public ConcurrentRedisChatMemory(Object memoryId, ChatMemoryStore chatMemoryStore) {
        this.memoryId = memoryId;
        this.chatMemoryStore = chatMemoryStore;
    }

    // 每次都强制从 Redis 读取最新消息（无缓存）
    @Override
    public List<ChatMessage> messages() {
        return chatMemoryStore.getMessages(memoryId);
    }

    // 直接追加 + 全量写入 Redis（无本地缓存）
    @Override
    public void add(ChatMessage message) {
        // 核心：读取并转为 可变ArrayList，解决不可变集合add报错
        List<ChatMessage> latestMessages = new ArrayList<>(chatMemoryStore.getMessages(memoryId));
        // 添加新消息
        latestMessages.add(message);
        // 同步写回 Redis
        chatMemoryStore.updateMessages(memoryId, latestMessages);
    }

    @Override
    public void clear() {
        chatMemoryStore.deleteMessages(memoryId);
    }

    @Override
    public Object id() {
        return memoryId;
    }
}
