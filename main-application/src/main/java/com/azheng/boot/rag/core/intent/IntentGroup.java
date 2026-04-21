package com.azheng.boot.rag.core.intent;

import java.util.List;

/**
 * 意图分组（MCP 与 KB）
 * MCP待扩展
 * @param kbIntents  KB 意图列表
 */
public record IntentGroup(List<NodeScore> kbIntents) {
}