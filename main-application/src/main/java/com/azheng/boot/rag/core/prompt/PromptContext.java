package com.azheng.boot.rag.core.prompt;

import cn.hutool.core.util.StrUtil;
import com.azheng.boot.rag.core.intent.NodeScore;
import com.azheng.boot.rag.core.rerank.ReRankContext;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 封装提示词渲染所需的上下文信息
 *
 */
@Data
@Builder
public class PromptContext {
    /**
     * 用户提问
     */
    private String question;


    /**
     * 这个是最终要构成提示词模版中的参考数据
     * 也就是检索阶段的最终数据
     */
    private String kbContext;

    /**
     * 意图识别筛选结果
     */
    private List<NodeScore> kbIntents;

    /**
     * 对应子问题的召回结果
     * String：nodeId List<ReCallContext>：可能存在有的
     */
    private Map<String, List<ReRankContext>> intentChunks;

    public boolean hasKb() {
        return StrUtil.isNotBlank(kbContext);
    }

}
