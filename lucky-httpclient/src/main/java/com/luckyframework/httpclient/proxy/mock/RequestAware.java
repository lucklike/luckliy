package com.luckyframework.httpclient.proxy.mock;


import com.luckyframework.httpclient.core.meta.Request;

/**
 * Request Aware
 */
public interface RequestAware {

    /**
     * 设置当前请求对象
     *
     * @param request 当前请求对象
     */
    void setRequest(Request request);

}
