package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;
import java.util.function.Function;

/**
 * 拦截器执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 07:15
 */
public class InterceptorPerformer {
    private final Function<MethodContext, Interceptor> interceptorFunction;
    private final Annotation interceptorRegisterAnn;
    private final Integer priority;

    public InterceptorPerformer(Function<MethodContext, Interceptor> interceptorFunction, Annotation interceptorRegisterAnn, Integer priority) {
        this.interceptorFunction = interceptorFunction;
        this.interceptorRegisterAnn = interceptorRegisterAnn;
        this.priority = priority;
    }

    public InterceptorPerformer(Function<MethodContext, Interceptor> interceptorFunction, Integer priority) {
        this(interceptorFunction, null, priority);
    }

    public InterceptorPerformer(Interceptor interceptor, Annotation interceptorRegisterAnn, Integer priority) {
        this(context -> interceptor, interceptorRegisterAnn, priority);
    }

    public InterceptorPerformer(Interceptor interceptor, Annotation interceptorRegisterAnn) {
        this(interceptor, interceptorRegisterAnn, interceptor.priority());
    }

    public InterceptorPerformer(Interceptor interceptor, Integer priority) {
        this(interceptor, null, priority);
    }

    public InterceptorPerformer(Interceptor interceptor) {
        this(interceptor, null, interceptor.priority());
    }

    /**
     * 获取拦截器的优先级
     *
     * @param context 拦截器上注解下文
     * @return 拦截器的优先级
     */
    public int getPriority(MethodContext context) {
        return this.priority == null ? getInterceptor(context).priority() : priority;
    }


    /**
     * 请求执行之前会执行该方法
     *
     * @param request 请求对象
     * @param context 拦截器上注解下文
     */
    public void beforeExecute(Request request, MethodContext context) {
        getInterceptor(context).beforeExecute(request, new InterceptorContext(context, interceptorRegisterAnn));
    }

    /**
     * 当代理方法为非void方法正常返回时，会执行此处的方法
     *
     * @param response 响应数据
     * @param context  响应拦截器注解上下文
     */
    public Response afterExecute(Response response, MethodContext context) {
        return getInterceptor(context).afterExecute(response, new InterceptorContext(context, interceptorRegisterAnn));
    }

    public Interceptor getInterceptor(MethodContext context) {
        return interceptorFunction.apply(context);
    }
}
