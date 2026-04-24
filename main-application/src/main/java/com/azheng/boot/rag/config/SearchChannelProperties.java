package com.azheng.boot.rag.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag.search")
public class SearchChannelProperties {

    private Channels channels = new Channels();


    @Data
    public static class Channels{

        private final VectorGlobal vectorGlobal = new VectorGlobal();

        private final IntentDirected intentDirected = new IntentDirected();

    }

    @Data
    public static class VectorGlobal{


        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 检索向量置信度阈值
         */
        private double confidenceThreshold = 0.6;


        /**
         * TopK 倍数
         * 全局检索时召回更多候选，后续通过 Rerank 筛选
         */
        private int topKMultiplier = 3;
    }

    @Data
    public static class IntentDirected{


        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 最低意图分数阈值
         */
        private double minIntentScore = 0.4;

        /**
         * TopK 倍数
         * 全局检索时召回更多候选，后续通过 Rerank 筛选
         */
        private int topKMultiplier = 2;
    }

}
