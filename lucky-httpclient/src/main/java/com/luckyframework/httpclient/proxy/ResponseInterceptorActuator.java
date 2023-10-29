package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Response;

import java.lang.annotation.Annotation;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 07:35
 */
public class ResponseInterceptorActuator extends InterceptorActuator {

    private final ResponseInterceptor interceptor;
    private final Annotation annotation;
    private final Integer annPriority;

    private ResponseInterceptorActuator(int type, ResponseInterceptor interceptor, Annotation annotation, Integer annPriority) {
        super(type);
        this.interceptor = interceptor;
        this.annotation = annotation;
        this.annPriority = annPriority;
    }

    public static ResponseInterceptorActuator createNullType(ResponseInterceptor interceptor) {
        return new ResponseInterceptorActuator(InterceptorActuator.TYPE_NULL, interceptor, null, null);
    }

    public static ResponseInterceptorActuator createClassType(ResponseInterceptor interceptor, Annotation annotation, Integer annPriority) {
        return new ResponseInterceptorActuator(InterceptorActuator.TYPE_CLASS, interceptor, annotation, annPriority);
    }

    public static ResponseInterceptorActuator createMethodType(ResponseInterceptor interceptor, Annotation annotation, Integer annPriority) {
        return new ResponseInterceptorActuator(InterceptorActuator.TYPE_METHOD, interceptor, annotation, annPriority);
    }

    public void activate(Response response, MethodContext methodContext) {
        interceptor.responseProcess(response, methodContext, annotation);
//        if (isNullType()) {
//            interceptor.responseProcess(response, null, annotation);
//        } else if (isClassType()) {
//            interceptor.responseProcess(response, methodContext.getClassContext(), annotation);
//        } else {
//            interceptor.responseProcess(response, methodContext, annotation);
//        }
    }

    public int priority() {
        if (annPriority == null) {
            return interceptor.responsePriority();
        }
        return annPriority;
    }
}
