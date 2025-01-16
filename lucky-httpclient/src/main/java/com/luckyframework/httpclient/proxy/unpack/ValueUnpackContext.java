package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.ValueContext;

import java.lang.annotation.Annotation;

/**
 * ValueUnpack上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/17 00:56
 */
public class ValueUnpackContext extends AnnotationContext {

    public ValueUnpackContext(ValueContext context, Annotation annotation) {
        setAnnotation(annotation);
        setContext(context);
    }

    @Override
    public ValueContext getContext() {
        return (ValueContext) super.getContext();
    }
}
