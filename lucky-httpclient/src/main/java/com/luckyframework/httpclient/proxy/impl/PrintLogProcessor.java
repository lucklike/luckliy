package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.RequestAfterProcessor;
import com.luckyframework.httpclient.proxy.ResponseAfterProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * 打印请求日志的后缀请求处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/6 11:20
 */
public class PrintLogProcessor implements RequestAfterProcessor, ResponseAfterProcessor {

    private static final Logger log = LoggerFactory.getLogger(PrintLogProcessor.class);

    @Override
    public void requestProcess(Request request, Annotation requestAfterHandleAnn) {
        log.info(">>> The request currently being executed is: {}", request.toString());
    }

    @Override
    public void responseProcess(Response response, Annotation responseAfterHandleAnn) {

        log.info("<<< The response status code currently returned is [{}] and the result is :{}", response.getState(), response.getStringResult());
    }

}
