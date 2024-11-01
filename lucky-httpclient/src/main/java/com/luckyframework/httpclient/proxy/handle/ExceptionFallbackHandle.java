package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.exception.LuckyRuntimeException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.reflect.MethodUtils;

import java.lang.annotation.Annotation;

/**
 * 支持降级处理的异常处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/01 16:38
 */
public class ExceptionFallbackHandle implements HttpExceptionHandle {

    @Override
    public Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        Annotation exHandAnn = methodContext.getMergedAnnotationCheckParent(ExceptionHandleMeta.class);
        ExceptionFallback fallbackAnn = methodContext.toAnnotation(exHandAnn, ExceptionFallback.class);
        Class<?> proxyClass = methodContext.getClassContext().getCurrentAnnotatedElement();

        String fallbackInstanceEx = fallbackAnn.value();
        if (StringUtils.hasText(fallbackInstanceEx)) {
            Object fallbackInstance = methodContext.parseExpression(fallbackInstanceEx, proxyClass);
            return MethodUtils.invoke(fallbackInstance, methodContext.getCurrentAnnotatedElement(), methodContext.getArguments());
        }

        ObjectGenerate fallbackGenerate = fallbackAnn.fallback();
        Class<?> clazz = fallbackGenerate.clazz();
        if (clazz == Object.class) {
            throw new LuckyRuntimeException("未检测到任何回滚配置");
        }
        Object fallbackInstance = methodContext.generateObject(fallbackGenerate);
        if (!proxyClass.isAssignableFrom(fallbackInstance.getClass())) {
            throw new LuckyRuntimeException("配置的回滚类与代理类类型不兼容");
        }
        return MethodUtils.invoke(fallbackInstance, methodContext.getCurrentAnnotatedElement(), methodContext.getArguments());
    }
}
