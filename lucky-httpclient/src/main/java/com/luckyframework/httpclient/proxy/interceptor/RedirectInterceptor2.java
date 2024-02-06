package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseProcessor;
import com.luckyframework.httpclient.core.VoidResponse;

/**
 * 通用重定向拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/6 22:57
 */
public class RedirectInterceptor2 implements Interceptor {


    @Override
    public void beforeExecute(Request request, InterceptorContext context) {
        Interceptor.super.beforeExecute(request, context);
    }

    @Override
    public VoidResponse afterExecute(VoidResponse voidResponse, ResponseProcessor responseProcessor, InterceptorContext context) {
        return Interceptor.super.afterExecute(voidResponse, responseProcessor, context);
    }

    @Override
    public Response afterExecute(Response response, InterceptorContext context) {
        return Interceptor.super.afterExecute(response, context);
    }
}
