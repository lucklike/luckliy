package com.luckyframework.proxy.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;

/**
 * A default {@link AsyncUncaughtExceptionHandler} that simply logs the exception.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public class SimpleAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Log logger = LogFactory.getLog(SimpleAsyncUncaughtExceptionHandler.class);


    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        if (logger.isErrorEnabled()) {
            logger.error("Unexpected exception occurred invoking async method: " + method, ex);
        }
    }

}