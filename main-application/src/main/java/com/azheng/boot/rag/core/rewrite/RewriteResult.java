package com.azheng.boot.rag.core.rewrite;

import java.util.List;

/**
 * @param rewrittenQuestion 精简原问题
 * @param subQuestions 扩展子问题
 */
public record RewriteResult(String rewrittenQuestion, List<String> subQuestions) {

}