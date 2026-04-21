package com.azheng.boot.rag.controller.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ConversationMessageVO {

    private String id;

    private String conversationId;

    private String userId;

    private String role;

    private String content;

    private Date createTime;

    private Integer delFlag;
}
