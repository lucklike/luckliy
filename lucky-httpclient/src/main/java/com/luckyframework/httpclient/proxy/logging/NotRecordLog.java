package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 不记录日志的日志处理器
 */
public class NotRecordLog implements LoggerHandler {

    public static final NotRecordLog INSTANCE = new NotRecordLog();

    private NotRecordLog () {}

    @Override
    public void recordRequestLog(MethodContext context, Request request) {

    }

    @Override
    public void recordMetaResponseLog(MethodContext context, Response response) {

    }

    @Override
    public void recordFinalResponseLog(MethodContext context, Response response) {

    }

}
