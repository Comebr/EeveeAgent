package com.azheng.framework.web;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 全局统一返回结果对象Result
 * 规范所有API的返回格式，避免开发人员随便自定义返回
 * @param <T>
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    /**
     * 成功状态码
     */
    public static final String  SUCCESS_CODE = "0";

    /**
     * 状态码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     * 使用泛型接收多种类型、如Object、String、List...
     */
    private T data;

    /**
     * 判断请求是否成功
     *
     * @return 如果状态码为 {@link #SUCCESS_CODE}，返回 {@code true}；否则返回 {@code false}
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }

}
