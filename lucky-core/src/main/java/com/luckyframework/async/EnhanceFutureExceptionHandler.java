package com.luckyframework.async;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/13 12:25
 */
@FunctionalInterface
public interface EnhanceFutureExceptionHandler {

    void handleException(Throwable tx);
}
