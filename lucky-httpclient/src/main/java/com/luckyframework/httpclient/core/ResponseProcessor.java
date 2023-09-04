package com.luckyframework.httpclient.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 响应处理器
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/6/18 16:17
 */
@FunctionalInterface
public interface ResponseProcessor {

    Logger log = LoggerFactory.getLogger(ResponseProcessor.class);

    ResponseProcessor DO_NOTHING_PROCESSOR = (r, i, h, f) -> {
        if (i != 200){
            log.warn("Unsuccessful return code [{}], the current request is: {}", i, r);
        }
    };

    void process(Request request, int status, HttpHeaderManager respHeader, InputStreamFactory inputStreamFactory);

    default void exceptionHandler(Request request, Exception e) {
        if (e instanceof HttpExecutorException) {
            throw (HttpExecutorException)e;
        }
        throw new HttpExecutorException("An exception occurred while executing an HTTP request: " + request, e);
    }

}
