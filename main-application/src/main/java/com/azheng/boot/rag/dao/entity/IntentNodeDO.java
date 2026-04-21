/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azheng.boot.rag.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("intent_node")
public class IntentNodeDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联知识库id
     */
    private Long kbId;

    /**
     * 意图节点唯一标识
     */
    private String intentCode;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 层级：0=DOMAIN,1=CATEGORY,2=INTENT
     */
    private Integer level;

    /**
     * 父节点唯一标识，默认null：ROOT
     */
    private String parentCode;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 示例问题
     */
    private String examples;

    /**
     * 类型：0=RAG知识库类 1=System系统交互类
     */
    private Integer kind;

    /**
     * 关联的Collection表名称
     */
    private String collectionName;

    /**
     * 知识库检索粗筛TopK，null：全局统一TopK
     */
    private Integer topK;

    /**
     * 提示词片段
     */
    private String promptSnippet;

    /**
     * 提示词模版
     */
    private String promptTemplate;

    /**
     * 排序字段
     */
    private Integer sortOrder;

    /**
     * 是否启用 1：启用 0：禁用
     */
    private Integer enabled;

    private String createBy;
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}