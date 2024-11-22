package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;

/**
 * 拦截器注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 02:59
 */
public class InterceptorContext extends AnnotationContext {

    public InterceptorContext(MethodContext methodContext, Annotation interceptorRegisterAnn){
        setAnnotation(interceptorRegisterAnn);
        setContext(methodContext);
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }
}
