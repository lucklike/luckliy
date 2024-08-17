package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Mock注解上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/2 09:28
 */
public class MockContext extends AnnotationContext {

    public MockContext(MethodContext methodContext, Annotation annotation) {
        setAnnotation(annotation);
        setContext(methodContext);
        setContextVar();
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }

    public Type getRealMethodReturnType() {
        return getContext().getRealMethodReturnType();
    }
}
