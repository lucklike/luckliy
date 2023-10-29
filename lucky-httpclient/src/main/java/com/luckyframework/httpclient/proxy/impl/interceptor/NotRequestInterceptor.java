package com.luckyframework.httpclient.proxy.impl.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.Context;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.RequestInterceptor;

import java.lang.annotation.Annotation;

/**
 * 不做任何处理的请求拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 04:38
 */
public class NotRequestInterceptor implements RequestInterceptor {

    @Override
    public void requestProcess(Request request, MethodContext context, Annotation requestAfterHandleAnn) {

    }
}
