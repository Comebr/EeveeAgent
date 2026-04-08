package com.azheng.boot.kb.controller.request;

import lombok.Data;

@Data
public class MkdirChunkTextRequest {

    private String chunkId;

    private String chunkText;
}
