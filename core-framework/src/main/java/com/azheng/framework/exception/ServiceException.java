package com.azheng.framework.exception;

import com.azheng.framework.errorcode.BaseErrorCode;
import com.azheng.framework.errorcode.IErrorCode;

import java.util.Optional;

public class ServiceException extends AbstractException{

    public ServiceException(String message) {
        this(message,null,BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(IErrorCode errorCode) {
        this(null,errorCode);
    }

    public ServiceException(String message,IErrorCode errorCode) {
        this(message,null,errorCode);
    }


    /**
     * @param message   自己写的错误详情
     * @param throwable 原始的异常对象
     * @param errorCode 预定义的错误码枚举
     */
    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
