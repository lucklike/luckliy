package com.luckyframework.httpclient.proxy.processor;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;

import java.lang.annotation.Annotation;

/**
 * 响应处理器注解上下文
 *
 * @author fukang
 * @version 2.1.1
 * @date 2024/05/24 11:24
 */
public class ProcessorAnnContext extends AnnotationContext {

    public ProcessorAnnContext(MethodContext methodContext, Annotation staticParamAnn) {
        setContext(methodContext);
        setAnnotation(staticParamAnn);
        setContextVar();
    }

    @Override
    public MethodContext getContext() {
        return (MethodContext) super.getContext();
    }

}
