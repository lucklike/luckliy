package com.luckyframework.httpclient.proxy.ssl;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/28 00:46
 */
public class SSLAnnotationContext extends AnnotationContext {
    public SSLAnnotationContext(MethodContext methodContext, Annotation staticParamAnn) {
        setContext(methodContext);
        setAnnotation(staticParamAnn);
        setContextVar();
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }
}
