package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Request;

import java.lang.annotation.Annotation;

/**
 * 请求拦截器执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 07:13
 */
public class RequestInterceptorActuator extends InterceptorActuator {

    private final RequestInterceptor interceptor;
    private final Annotation annotation;
    private final Integer annPriority;

    private RequestInterceptorActuator(int type, RequestInterceptor interceptor, Annotation annotation, Integer annPriority) {
        super(type);
        this.interceptor = interceptor;
        this.annotation = annotation;
        this.annPriority = annPriority;
    }

    public static RequestInterceptorActuator createNullType(RequestInterceptor interceptor) {
        return new RequestInterceptorActuator(InterceptorActuator.TYPE_NULL, interceptor, null, null);
    }

    public static RequestInterceptorActuator createClassType(RequestInterceptor interceptor, Annotation annotation, Integer annPriority) {
        return new RequestInterceptorActuator(InterceptorActuator.TYPE_CLASS, interceptor, annotation, annPriority);
    }

    public static RequestInterceptorActuator createMethodType(RequestInterceptor interceptor, Annotation annotation, Integer annPriority) {
        return new RequestInterceptorActuator(InterceptorActuator.TYPE_METHOD, interceptor, annotation, annPriority);
    }


    public void activate(Request request, MethodContext methodContext) {
        interceptor.requestProcess(request, methodContext, annotation);
//        if (isNullType()) {
//            interceptor.requestProcess(request, null, annotation);
//        } else if (isClassType()) {
//            interceptor.requestProcess(request, methodContext.getClassContext(), annotation);
//        } else {
//            interceptor.requestProcess(request, methodContext, annotation);
//        }
    }

    public int priority() {
        if (annPriority == null) {
            return interceptor.requestPriority();
        }
        return annPriority;
    }
}
