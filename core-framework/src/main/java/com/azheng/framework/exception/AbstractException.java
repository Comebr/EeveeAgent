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
package com.azheng.framework.exception;

import com.azheng.framework.errorcode.IErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 抽象异常类，继承运行时异常
 * 目前包含两类异常对象：客户端异常、服务端异常
 * @Getter 支持从外界获取内部参数
 */
@Getter
public abstract class AbstractException extends RuntimeException {
    public final String errorCode;

    public final String errorMessage;

    /**
     * @param message 自己写的错误详情
     * @param throwable 原始的异常对象
     * @param errorCode 预定义的错误码枚举
     */
    public AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        // 把错误描述与根因传给父类RuntimeException
        super(message,throwable);
        // 保存错误码
        this.errorCode = errorCode.code();
        // 如果传了自定义 message 就用传的，没传就用 errorCode 里默认的
        this.errorMessage = Optional.ofNullable(StringUtils.hasLength(message) ? message : null).orElse(errorCode.message());
    }
}
