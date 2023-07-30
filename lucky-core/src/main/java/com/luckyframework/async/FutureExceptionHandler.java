package com.luckyframework.async;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 04:19
 */
@FunctionalInterface
public interface FutureExceptionHandler {

    void handleException(Throwable tx);
}
