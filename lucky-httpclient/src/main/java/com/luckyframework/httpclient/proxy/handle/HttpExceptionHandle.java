package com.luckyframework.httpclient.proxy.handle;


import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

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
     * 处理异常，该处理器可以返回一个结果，最终这个结果最终会作为代理方法的返回结果
     *
     * @param methodContext 方法上下文
     * @param request       请求实例
     * @param throwable     异常实例
     */
    Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable);
}
