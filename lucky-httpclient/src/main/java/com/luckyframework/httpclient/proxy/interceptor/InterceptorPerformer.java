package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;

/**
 * 拦截器执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 07:15
 */
public class InterceptorPerformer {
    private final Interceptor interceptor;
    private final Annotation interceptorRegisterAnn;
    private final int priority;

    protected InterceptorPerformer(Interceptor interceptor, Annotation interceptorRegisterAnn, int priority) {
        this.interceptor = interceptor;
        this.interceptorRegisterAnn = interceptorRegisterAnn;
        this.priority = priority;
    }

    protected InterceptorPerformer(Interceptor interceptor, Annotation interceptorRegisterAnn) {
        this(interceptor, interceptorRegisterAnn, interceptor.priority());
    }

    /**
     * 获取拦截器的优先级
     *
     * @return 拦截器的优先级
     */
    public int getPriority() {
        return this.priority;
    }


    /**
     * 请求执行之前会执行该方法
     *
     * @param request 请求对象
     * @param context 拦截器上注解下文
     */
    public void beforeExecute(Request request, MethodContext context) {
        interceptor.beforeExecute(request, new InterceptorContext(context, interceptorRegisterAnn));
    }

    /**
     * 当代理方法为void方法正常返回时，会执行此处的方法
     *
     * @param voidResponse      void方法响应
     * @param responseProcessor 响应结果处理器
     * @param context           响应拦截器注解上下文
     */
    public VoidResponse afterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, MethodContext context) {
        return interceptor.afterExecute(voidResponse, responseProcessor, new InterceptorContext(context, interceptorRegisterAnn));
    }

    /**
     * 当代理方法为非void方法正常返回时，会执行此处的方法
     *
     * @param response 响应数据
     * @param context  响应拦截器注解上下文
     */
    public Response afterExecute(Response response, MethodContext context) {
        return interceptor.afterExecute(response, new InterceptorContext(context, interceptorRegisterAnn));
    }
}
