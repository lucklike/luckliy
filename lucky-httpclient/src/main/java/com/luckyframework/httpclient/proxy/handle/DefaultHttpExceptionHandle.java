package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;
import com.luckyframework.httpclient.proxy.exeception.LuckyProxyMethodExecuteException;
import com.luckyframework.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable) throws Throwable {
        return exceptionHandler(methodContext, throwable);
    }

    public static Object exceptionHandler(MethodContext methodContext, Throwable throwable) throws Throwable {
        if (throwable instanceof ActivelyThrownException && throwable.getCause() != null) {
            Throwable cause = throwable.getCause();
            log.error("HTTP proxy method ['{}'] execution failed.", MethodUtils.getLocation(methodContext.getCurrentAnnotatedElement()), cause);
            throw cause;
        }

        throw new LuckyProxyMethodExecuteException(throwable, "HTTP proxy method ['{}'] execution failed.", MethodUtils.getLocation(methodContext.getCurrentAnnotatedElement())).error(log);
    }
}
