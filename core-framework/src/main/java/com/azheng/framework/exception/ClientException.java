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

import com.azheng.framework.errorcode.BaseErrorCode;
import com.azheng.framework.errorcode.IErrorCode;

import java.util.Optional;


/**
 * 客户端异常
 * 用户发起调用请求后因客户端提交参数或其他客户端问题导致的异常
 */
public class ClientException extends AbstractException {
    //this重载父类构造方法，区分多种情况

    /**
     * 只提供错误消息，使用默认错误码
     */
    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }


    /**
     * 只提供状态码，而且还只能是系统定义的状态码
     */
    public ClientException(IErrorCode errorCode) {
        this(null,errorCode);
    }

    /**
     * 提供错误消息及错误码
     */
    public ClientException(String message,IErrorCode errorCode) {
        this(message,null,errorCode);
    }

    /**
     * 父类的构造方法
     */
    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
