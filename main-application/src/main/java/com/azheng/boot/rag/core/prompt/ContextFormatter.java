package com.azheng.boot.rag.core.prompt;



import com.azheng.boot.rag.core.intent.NodeScore;
import com.azheng.boot.rag.core.rerank.ReRankContext;

import java.util.List;
import java.util.Map;

public interface ContextFormatter {

    String formatKbContext(List<NodeScore> kbIntents, Map<String, List<ReRankContext>> rerankedByIntent, int topK);
}
