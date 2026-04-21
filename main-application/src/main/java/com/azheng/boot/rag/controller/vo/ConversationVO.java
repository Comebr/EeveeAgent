package com.azheng.boot.rag.controller.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ConversationVO {

    private String id;

    private String conversationId;

    private String userId;

    private String title;

    private Date lastTalkTime;

    private Integer delFlag;
}
