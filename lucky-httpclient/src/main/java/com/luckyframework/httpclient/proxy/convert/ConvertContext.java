package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * 转换器注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/2 09:28
 */
public class ConvertContext extends AnnotationContext {

    public ConvertContext(MethodContext methodContext, Annotation annotation) {
        setAnnotation(annotation);
        setContext(methodContext);
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }

    public Type getRealMethodReturnType() {
        return getContext().getRealMethodReturnType();
    }
}
