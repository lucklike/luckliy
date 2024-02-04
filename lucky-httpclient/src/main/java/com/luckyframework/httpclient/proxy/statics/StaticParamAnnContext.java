package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;

/**
 * 静态注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/31 16:15
 */
public class StaticParamAnnContext extends AnnotationContext {

    public StaticParamAnnContext(MethodContext methodContext, Annotation staticParamAnn) {
        setContext(methodContext);
        setAnnotation(staticParamAnn);
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }
}
