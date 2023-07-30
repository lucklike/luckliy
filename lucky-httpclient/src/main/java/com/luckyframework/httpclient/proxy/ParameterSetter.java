package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Request;

/**
 * 参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:21
 */
@FunctionalInterface
public interface ParameterSetter {

    /**
     * 请求参数设置
     *
     * @param request    请求体
     * @param paramName  待设置参数的名称
     * @param paramValue 待设置参数的值
     */
    void set(Request request, String paramName, Object paramValue);
}
