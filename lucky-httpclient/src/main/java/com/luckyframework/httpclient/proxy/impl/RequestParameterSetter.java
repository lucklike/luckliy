package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.ParameterSetter;

/**
 * Request参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class RequestParameterSetter implements ParameterSetter {


    @Override
    public void set(Request request, String paramName, Object paramValue) {
        request.addRequestParameter(paramName, paramValue);
    }
}
