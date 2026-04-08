package com.azheng.boot.rag.chunking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChunkOptions {
    /**
     * 分块基础参数配置
     */

    @Builder.Default
    private Integer maxChunkSize = 512;

    @Builder.Default
    private Integer maxOverlapSize = 0;
}


