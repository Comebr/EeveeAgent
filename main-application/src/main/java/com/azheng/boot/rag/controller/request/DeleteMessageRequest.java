package com.azheng.boot.rag.controller.request;

import lombok.Data;

@Data
public class DeleteMessageRequest {

    private String messageId;

    private String role;
}
