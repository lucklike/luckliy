package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 拦截器执行链
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 03:03
 */
public class InterceptorPerformerChain {

    private final List<InterceptorPerformer> interceptorPerformerList = new ArrayList<>();

    public void addInterceptor(Function<MethodContext, Interceptor> interceptorFunction, Annotation interceptorRegisterAnn, int priority) {
        interceptorPerformerList.add(new InterceptorPerformer(interceptorFunction, interceptorRegisterAnn, priority));
    }

    public void addInterceptorPerformers(InterceptorPerformer... interceptors) {
        interceptorPerformerList.addAll(Arrays.asList(interceptors));
    }

    public void addInterceptorPerformers(Collection<InterceptorPerformer> interceptors) {
        interceptorPerformerList.addAll(interceptors);
    }

    public void addInterceptor(Interceptor interceptor, Annotation interceptorRegisterAnn) {
        interceptorPerformerList.add(new InterceptorPerformer(interceptor, interceptorRegisterAnn));
    }

    public void addInterceptor(Interceptor interceptor, Annotation interceptorRegisterAnn, int priority) {
        interceptorPerformerList.add(new InterceptorPerformer(interceptor, interceptorRegisterAnn, priority));
    }

    public void addInterceptor(Interceptor interceptor) {
        interceptorPerformerList.add(new InterceptorPerformer(interceptor, (Integer) null));
    }

    public void addInterceptors(Collection<Interceptor> interceptor) {
        for (Interceptor inter : interceptor) {
            addInterceptor(inter);
        }
    }

    public void addInterceptor(InterceptorRegister interceptorRegisterAnn, Context context) {
        int interceptorPriority = interceptorRegisterAnn.priority();
        addInterceptor(c -> context.generateObject(interceptorRegisterAnn.intercept()), interceptorRegisterAnn, interceptorPriority);
    }

    /**
     * 按照优先级排序
     */
    public void sort(MethodContext context) {
        interceptorPerformerList.sort(Comparator.comparingInt(ip -> ip.getPriority(context)));
    }


    /**
     * 请求执行之前会执行该方法
     *
     * @param request 请求对象
     * @param context 拦截器上注解下文
     */
    public void beforeExecute(Request request, MethodContext context) {
        for (InterceptorPerformer interceptorPerformer : interceptorPerformerList) {
            interceptorPerformer.beforeExecute(request, context);
        }
    }

    /**
     * 当代理方法为非void方法正常返回时，会执行此处的方法
     *
     * @param response 响应数据
     * @param context  响应拦截器注解上下文
     */
    public Response afterExecute(Response response, MethodContext context) {
        Response result = response;
        for (InterceptorPerformer interceptorPerformer : interceptorPerformerList) {
            result = interceptorPerformer.afterExecute(result, context);
        }
        return result;
    }

}
