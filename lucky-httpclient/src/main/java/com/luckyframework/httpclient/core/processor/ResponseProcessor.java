package com.luckyframework.httpclient.core.processor;

import com.luckyframework.httpclient.core.exception.HttpExecutorException;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.ResponseMetaData;
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

    void process(ResponseMetaData responseMetaData) throws Exception;

    default void exceptionHandler(Request request, Exception e) {
        if (e instanceof HttpExecutorException) {
            throw (HttpExecutorException) e;
        }
        throw new HttpExecutorException(e, "An exception occurred while executing the http request: [{}] {}", request.getRequestMethod(), request.getUrl());
    }

}
