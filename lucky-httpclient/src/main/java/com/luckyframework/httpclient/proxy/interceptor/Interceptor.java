package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.InterceptorProhibition;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;

import java.lang.annotation.Annotation;

/**
 * 拦截器，可以在请求前后执行一段特定的逻辑
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 02:34
 */
public interface Interceptor {

    /**
     * 请求执行之前会执行该方法
     *
     * @param request 请求对象
     * @param context 拦截器上注解下文
     */
    default void beforeExecute(Request request, InterceptorContext context) {
        if (isExecute(context)) {
            doBeforeExecute(request, context);
        }
    }

    default void doBeforeExecute(Request request, InterceptorContext context) {

    }

    //----------------------------------------------------------------//

    /**
     * 当代理方法为void方法正常返回时，会执行此处的方法
     *
     * @param voidResponse      void方法响应
     * @param responseProcessor 响应结果处理器
     * @param context           响应拦截器注解上下文
     */
    default VoidResponse afterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        if (isExecute(context)) {
            return doAfterExecute(voidResponse, responseProcessor, context);
        }
        return voidResponse;
    }

    default VoidResponse doAfterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        return voidResponse;
    }

    //----------------------------------------------------------------//

    /**
     * 当代理方法为非void方法正常返回时，会执行此处的方法
     *
     * @param response 响应数据
     * @param context  响应拦截器注解上下文
     */
    default Response afterExecute(Response response, InterceptorContext context) {
        if (isExecute(context)) {
            return doAfterExecute(response, context);
        }
        return response;
    }

    default Response doAfterExecute(Response response, InterceptorContext context) {
        return response;
    }

    //----------------------------------------------------------------//

    /**
     * 优先级，数值越高优先级越低
     */
    default int priority() {
        return Integer.MAX_VALUE;
    }

    /**
     * 当方法上存在该注解时不执行此拦截器的逻辑
     */
    default Class<? extends Annotation> prohibition() {
        return InterceptorProhibition.class;
    }

    default boolean isExecute(InterceptorContext context) {
        Class<? extends Annotation> prohibitionAnnType
                = context.notNullAnnotated()
                ? context.toAnnotation(InterceptorRegister.class).prohibition()
                : prohibition();
        return !context.isAnnotatedCheckParent(prohibitionAnnType);
    }
}
