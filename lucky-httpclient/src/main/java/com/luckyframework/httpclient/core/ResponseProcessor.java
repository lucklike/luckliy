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

    ResponseProcessor DO_NOTHING_PROCESSOR = rmd -> {
        if (!rmd.isSuccess()){
            log.warn("Unsuccessful return code [{}], the current request is: {}", rmd.getStatus(), rmd.getRequest());
        }
    };

    void process(ResponseMetaData responseMetaData);

    default void exceptionHandler(Request request, Exception e) {
        if (e instanceof HttpExecutorException) {
            throw (HttpExecutorException)e;
        }
        throw new HttpExecutorException("An exception occurred while executing an HTTP request: " + request, e);
    }

}
