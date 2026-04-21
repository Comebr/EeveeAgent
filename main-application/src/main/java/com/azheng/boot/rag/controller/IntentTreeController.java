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

package com.azheng.boot.rag.controller;

import com.azheng.boot.rag.controller.request.IntentNodeBatchRequest;
import com.azheng.boot.rag.controller.request.IntentNodeCreateRequest;
import com.azheng.boot.rag.controller.vo.IntentNodeTreeVO;
import com.azheng.boot.rag.controller.request.IntentNodeUpdateRequest;
import com.azheng.boot.rag.service.IntentTreeService;
import com.azheng.framework.web.Result;
import com.azheng.framework.web.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 意图树控制器
 * 提供意图节点树的查询、创建、更新和删除功能
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intent-tree")
public class IntentTreeController {

    private final IntentTreeService intentTreeService;

    /**
     * 获取完整的意图节点树
     */
    @GetMapping("/trees")
    public Result<List<IntentNodeTreeVO>> tree() {
        return Results.success(intentTreeService.getFullTree());
    }

    /**
     * 创建意图节点
     */
    @PostMapping("/createNode")
    public Result<String> createNode(@RequestBody IntentNodeCreateRequest requestParam) {
        return Results.success(intentTreeService.createNode(requestParam));
    }

    /**
     * 更新意图节点
     */
    @PutMapping("/mkdir/{id}")
    public Result<Void> updateNode(@PathVariable String id, @RequestBody IntentNodeUpdateRequest requestParam) {
        intentTreeService.updateNode(id, requestParam);
        return Results.success();
    }

    /**
     * 删除意图节点
     */
    @DeleteMapping("/remove/{id}")
    public Result<Void> deleteNode(@PathVariable String id) {
        intentTreeService.deleteNode(id);
        return Results.success();
    }

    /**
     * 批量启用节点
     */
    @PostMapping("/batch/enable")
    public Result<Void> batchEnable(@RequestBody IntentNodeBatchRequest requestParam) {
        intentTreeService.batchEnableNodes(requestParam.getIds());
        return Results.success();
    }

    /**
     * 批量停用节点
     */
    @PostMapping("/batch/disable")
    public Result<Void> batchDisable(@RequestBody IntentNodeBatchRequest requestParam) {
        intentTreeService.batchDisableNodes(requestParam.getIds());
        return Results.success();
    }

    /**
     * 批量删除节点
     */
    @PostMapping("/batch/delete")
    public Result<Void> batchDelete(@RequestBody IntentNodeBatchRequest requestParam) {
        intentTreeService.batchDeleteNodes(requestParam.getIds());
        return Results.success();
    }
}