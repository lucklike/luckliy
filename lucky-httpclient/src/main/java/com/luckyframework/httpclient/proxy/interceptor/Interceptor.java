package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.InterceptorProhibition;
import com.luckyframework.httpclient.proxy.annotations.InterceptorRegister;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * 拦截器，可以在请求前后执行一段特定的逻辑
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 02:34
 */
public interface Interceptor {


    //----------------------------------------------------------------------
    //                      Before Execute
    //----------------------------------------------------------------------


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


    //----------------------------------------------------------------------
    //                      After Execute
    //----------------------------------------------------------------------


    /**
     * 当代理方法为非void方法正常返回时，会执行此处的方法
     *
     * @param response 响应数据
     * @param context  响应拦截器注解上下文
     */
    default Response afterExecute(Response response, InterceptorContext context) {
        if (isExecute(context)) {
            Response result = doAfterExecute(response, context);
            if (!Objects.equals(result, response)) {
                context.setResponseVar(result);
            }
            return result;
        }
        return response;
    }

    default Response doAfterExecute(Response response, InterceptorContext context) {
        return response;
    }

    //----------------------------------------------------------------------
    //                      Priority And Prohibition
    //----------------------------------------------------------------------

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
