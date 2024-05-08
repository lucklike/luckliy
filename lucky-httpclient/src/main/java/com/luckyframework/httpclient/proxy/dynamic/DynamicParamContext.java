package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.ValueContext;

import java.lang.annotation.Annotation;

/**
 * 动态注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/31 16:57
 */
public class DynamicParamContext extends AnnotationContext {

    public DynamicParamContext(ValueContext valueContext, Annotation dynamicAnnotation) {
        setContext(valueContext);
        setAnnotation(dynamicAnnotation);
        setContextVar();
    }

    @Override
    public ValueContext getContext() {
        return (ValueContext) super.getContext();
    }

    public Object getValue() {
        return getContext().getValue();
    }
}
