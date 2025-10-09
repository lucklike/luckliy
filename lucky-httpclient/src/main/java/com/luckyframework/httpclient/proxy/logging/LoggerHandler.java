package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.util.Objects;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_CONTENT_INIT_THREAD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_REQUEST_THREAD_$;

/**
 * 日志处理器
 */
public interface LoggerHandler {

    /**
     * 记录请求日志
     *
     * @param context 当前方法上下文
     * @param request 当前请求实例
     */
    void recordRequestLog(MethodContext context, Request request);

    /**
     * 记录元响应日志
     *
     * @param context  当前方法上下文
     * @param response 当前响应实例
     */
    void recordMetaResponseLog(MethodContext context, Response response);

    /**
     * 记录最终响应日志
     *
     * @param context  当前方法上下文
     * @param response 当前响应实例
     */
    void recordFinalResponseLog(MethodContext context, Response response);


    //-------------------------------------------------------------------------------------------------
    //                                  Util Method
    //-------------------------------------------------------------------------------------------------

    /**
     * 当前请求是否是异步执行的
     *
     * @param mc 方法上下文
     * @return 当前请求是否是异步执行的
     */
    default boolean isAsyncRequest(MethodContext mc) {
        Thread mcInitThread = mc.getRootVar($_METHOD_CONTENT_INIT_THREAD_$, Thread.class);
        Thread requestThread = mc.getRootVar($_REQUEST_THREAD_$, Thread.class);
        return !Objects.equals(mcInitThread, requestThread);
    }


}
