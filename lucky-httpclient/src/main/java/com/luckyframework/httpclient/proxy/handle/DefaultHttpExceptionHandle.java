package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.exeception.LuckyProxyMethodExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认的Http异常处理器，打印Request以及异常信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 23:04
 */
public class DefaultHttpExceptionHandle implements NotReturnExceptionHandle {
    private static final Logger log = LoggerFactory.getLogger(DefaultHttpExceptionHandle.class);

    @Override
    public void doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        throw new LuckyProxyMethodExecuteException(throwable, "The proxy method '{}' executes an exception.", methodContext.getCurrentAnnotatedElement()).printException(log);
    }
}
