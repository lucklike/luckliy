package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.Request;

import java.net.Proxy;

/**
 * 代理参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:21
 */
public class ProxyParameterSetter extends ValueNameParameterSetter {

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        request.setProxy(((Proxy) paramValue));
    }
}
