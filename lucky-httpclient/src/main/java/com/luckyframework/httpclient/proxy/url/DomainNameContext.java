package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;

/**
 * 域名注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/4 16:52
 */
public class DomainNameContext extends AnnotationContext {

    public DomainNameContext(MethodContext context, Annotation domainNameAnn) {
        setAnnotation(domainNameAnn);
        setContext(context);
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }
}
