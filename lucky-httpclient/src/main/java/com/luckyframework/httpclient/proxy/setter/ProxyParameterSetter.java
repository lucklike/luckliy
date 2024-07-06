package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.proxy.ProxyInfo;
import com.luckyframework.httpclient.core.meta.Request;

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
        request.setProxyInfo(((ProxyInfo) paramValue));
    }
}
