package com.luckyframework.httpclient.proxy;


import com.luckyframework.httpclient.core.Request;

/**
 * 异常处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/28 15:27
 */
@FunctionalInterface
public interface HttpExceptionHandle {

    /**
     * 处理异常
     *
     * @param request   请求实例
     * @param exception 异常实例
     */
    void exceptionHandler(Request request, Exception exception);
}
