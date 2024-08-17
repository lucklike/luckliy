package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;

@FunctionalInterface
public interface MockResponseFactory {

    /**
     * Mock响应实例生成方法
     *
     * @param request 请求实例
     * @param context Mock注解上下文
     * @return Mock响应实例
     */
    Response createMockResponse(Request request, MockContext context);
}
