package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 拦截器链
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 03:03
 */
public class InterceptorPerformerChain {

    private final List<InterceptorPerformer> interceptorPerformerList = new ArrayList<>();

    public void InterceptorPerformers(InterceptorPerformer... interceptors) {
        interceptorPerformerList.addAll(Arrays.asList(interceptors));
    }

    public void InterceptorPerformers(Collection<InterceptorPerformer> interceptors) {
        interceptorPerformerList.addAll(interceptors);
    }

    public void addInterceptor(Interceptor interceptor, Annotation interceptorRegisterAnn) {
        interceptorPerformerList.add(new InterceptorPerformer(interceptor, interceptorRegisterAnn));
    }

    public void addInterceptor(Interceptor interceptor, Annotation interceptorRegisterAnn, int priority) {
        interceptorPerformerList.add(new InterceptorPerformer(interceptor, interceptorRegisterAnn, priority));
    }

    public void addInterceptor(Interceptor interceptor) {
        interceptorPerformerList.add(new InterceptorPerformer(interceptor, null));
    }

    public void addInterceptors(Collection<Interceptor> interceptor) {
        for (Interceptor inter : interceptor) {
            addInterceptor(inter);
        }
    }

    /**
     * 按照优先级排序
     */
    public void sort() {
        interceptorPerformerList.sort(Comparator.comparingInt(InterceptorPerformer::getPriority));
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
     * 当代理方法为void方法正常返回时，会执行此处的方法
     *
     * @param voidResponse      void方法响应
     * @param responseProcessor 响应结果处理器
     * @param context           响应拦截器注解上下文
     */
    public VoidResponse afterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, MethodContext context) {
        VoidResponse result = voidResponse;
        for (InterceptorPerformer interceptorPerformer : interceptorPerformerList) {
            result = interceptorPerformer.afterExecute(result, responseProcessor, context);
        }
        return result;
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
