package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/21 04:46
 */
public class HttpRequestContext extends AnnotationContext {

    public HttpRequestContext(MethodContext context, Annotation domainNameAnn) {
        setAnnotation(domainNameAnn);
        setContext(context);
        setContextVar();
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }
}
