package com.luckyframework.httpclient.proxy.destroy;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;

/**
 * 销毁上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/18 00:57
 */
public class DestroyContext extends AnnotationContext {

    public DestroyContext(MethodContext context, Annotation annotation) {
        setContext(context);
        setAnnotation(annotation);
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }
}
