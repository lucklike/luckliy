package com.luckyframework.httpclient.core;

/**
 * 响应处理器
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/6/18 16:17
 */
@FunctionalInterface
public interface ResponseProcessor {

    ResponseProcessor DO_NOTHING_PROCESSOR = (i, h, f) -> {};

    void process(int status, HttpHeaderManager header, InputStreamFactory inputStreamFactory);

    default void exceptionHandler(Request request, Exception e) {
        if (e instanceof HttpExecutorException) {
            throw (HttpExecutorException)e;
        }
        throw new HttpExecutorException("An exception occurred while executing an HTTP request: " + request, e);
    }

}
