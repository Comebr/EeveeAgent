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
