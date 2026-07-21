package com.example.rag_qa_system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理 — 统一返回错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return CommonResult.error(50000, "系统繁忙，请稍后重试");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResult<?> handleIllegalArgument(IllegalArgumentException e) {
        return CommonResult.error(40000, e.getMessage());
    }
}