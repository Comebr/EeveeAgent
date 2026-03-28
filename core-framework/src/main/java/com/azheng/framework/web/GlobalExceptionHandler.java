package com.azheng.framework.web;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.azheng.framework.errorcode.BaseErrorCode;
import com.azheng.framework.exception.AbstractException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    /**
     * 处理应用内抛出的异常
     * @param request --当前请求对象，用于获取请求信息
     * @param e --捕获到的异常对象
     * @return
     */
    @ExceptionHandler(value = AbstractException.class)
    public Result<Void> abstractExceptionHandler(HttpServletRequest request,AbstractException e){
        // 1.有Throwable根因
        if(e.getCause()!=null){
            // a.日志打印：请求方法、请求url、异常对象、根因
            log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), e, e.getCause());
            return Results.failure(e);
        }

        // 2.无具体根因：主动构建堆栈跟踪信息
        StringBuilder stringBuilder = new StringBuilder();
        // a.拼接异常类名和错误消息
        stringBuilder.append(e.getClass().getName())
                    .append(": ")
                    .append(e.getErrorMessage())
                    .append("\n");
        // b.获取异常的堆栈跟踪数组
        StackTraceElement[] stackTrace = e.getStackTrace();
        // c.拼接堆栈的5条以内的信息
        for (int i = 0; i < Math.min(5,stackTrace.length); i++) {
            stringBuilder.append("\tat  ").append(stackTrace[i]).append("\n");
        }
        // d.日志记录
        log.error("[{}] {} [ex] {} \n\n{}", request.getMethod(), request.getRequestURL().toString(), e, stringBuilder);

        return Results.failure(e);
    }

    /**
     * 拦截处理未捕获异常
     */
    @ExceptionHandler(value = Throwable.class)
    public Result<Void> unCaughtExceptionHandler(HttpServletRequest request , Throwable throwable){
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        return Results.failure();
    }



    /**
     * 拦截参数验证异常
     * @param request
     * @param ex
     */
    @SneakyThrows
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<Void> validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        FieldError firstFieldError = CollectionUtil.getFirst(bindingResult.getFieldErrors());
        String exceptionStr = Optional.ofNullable(firstFieldError)
                .map(FieldError::getDefaultMessage)
                .orElse(StrUtil.EMPTY);
        log.error("[{}] {} [ex] {}", request.getMethod(),getUrl(request), exceptionStr);
        return Results.failure(BaseErrorCode.CLIENT_ERROR.code(), exceptionStr);
    }

    /**
     * 从HTTP请求对象中获取完整拼接URL
     * @param request
     * @return
     */
    private String getUrl(HttpServletRequest request) {
        if (StrUtil.isBlank(request.getQueryString())) {
            return request.getRequestURL().toString();
        }
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }
}