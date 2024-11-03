package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 熔断器接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/4 02:32
 */
public interface FuseProtector {

    /**
     * 是否需要进行熔断
     *
     * @param methodContext 方法上下文对象
     * @param request       当前请求对象
     * @return 是否需要进行熔断
     */
    boolean fuseOrNot(MethodContext methodContext, Request request);

    /**
     * 记录发生异常时的快照信息
     *
     * @param methodContext 方法上下文对象
     * @param request       当前请求对象
     * @param throwable     异常实例
     */
    void record(MethodContext methodContext, Request request, Throwable throwable);
}
