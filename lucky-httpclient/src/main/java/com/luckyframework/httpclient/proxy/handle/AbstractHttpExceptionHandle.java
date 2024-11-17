package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.exeception.LuckyProxyMethodExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * 通用异常处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/4 00:36
 */
public abstract class AbstractHttpExceptionHandle implements HttpExceptionHandle {

    private static final Logger log = LoggerFactory.getLogger(AbstractHttpExceptionHandle.class);

    @Override
    public Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {

        // 获取异常注解
        ExceptionHandleMeta exceptionHandleMetaAnn = methodContext.getMergedAnnotationCheckParent(ExceptionHandleMeta.class);

        // 校验条件表达式，如果条件表达式存在且条件不成立时直接走默认处理
        String condition = exceptionHandleMetaAnn.condition();
        if (StringUtils.hasText(condition) && !methodContext.parseExpression(condition, boolean.class)) {
            return throwExceptionPrintLog(methodContext, throwable);
        }

        // 校验异常，当当前异常不再配置的异常范围内时直接走默认处理
        Class<? extends Throwable>[] exceptions = exceptionHandleMetaAnn.exceptions();
        if (!ExceptionUtils.isAssignableFrom(Arrays.asList(exceptions), throwable.getClass())) {
            return throwExceptionPrintLog(methodContext, throwable);
        }

        // 条件表达式和异常校验均通过时才进行处理
        return doExceptionHandler(methodContext, request, throwable);
    }

    /**
     * 抛出异常并打印日志
     *
     * @param methodContext 方法上下文
     * @param throwable     异常实例
     * @return 返回值
     */
    protected Object throwExceptionPrintLog(MethodContext methodContext, Throwable throwable) {
        throw new LuckyProxyMethodExecuteException(throwable, "The proxy method '{}' executes an exception.", methodContext.getCurrentAnnotatedElement()).printException(log);
    }

    /**
     * 处理异常，该处理器可以返回一个结果，最终这个结果最终会作为代理方法的返回结果
     *
     * @param methodContext 方法上下文
     * @param request       请求实例
     * @param throwable     异常实例
     */
    protected abstract Object doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable);

}
