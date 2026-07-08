package com.luckyframework.httpclient.proxy.configapi.parse;

import com.luckyframework.httpclient.core.meta.Request;

/**
 * 请求参数信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/7/9 00:41
 */
public class RequestParamInfo {

    private final String name;
    private final Object value;
    private final Request request;

    private RequestParamInfo(String name, Object value, Request request) {
        this.name = name;
        this.value = value;
        this.request = request;
    }

    public static RequestParamInfo of(String name, Object value, Request request) {
        return new RequestParamInfo(name, value, request);
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public Request getRequest() {
        return request;
    }
}
