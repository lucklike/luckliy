package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.exeception.LuckyProxyMethodExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 默认的Http异常处理器，打印Request以及异常信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 23:04
 */
public class DefaultHttpExceptionHandle implements HttpExceptionHandle {
    private static final Logger log = LoggerFactory.getLogger(DefaultHttpExceptionHandle.class);

    @Override
    public Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        return exceptionHandler(methodContext, throwable);
    }

    public static Object exceptionHandler(MethodContext methodContext, Throwable throwable) {
        if ((throwable instanceof ActivelyThrownException) && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        ClassContext classContext = methodContext.lookupContext(ClassContext.class);
        Class<?> clazz = classContext.getCurrentAnnotatedElement();
        Method method = methodContext.getCurrentAnnotatedElement();

        String errMsg = StringUtils.format("HTTP proxy method ['{}#{}()'] execution failed.", clazz.getName(), method.getName());

        if (throwable instanceof RuntimeException) {
            log.error(errMsg, throwable);
            throw (RuntimeException) throwable;
        }

        throw new LuckyProxyMethodExecuteException(throwable, errMsg).printException(log);
    }
}
