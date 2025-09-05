package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Version;


/**
 * 协议版本设置
 *
 * @author fukang
 * @version 3.0.2
 * @date 2025/9/5 00:21
 */
public class HttpVersionParameterSetter extends ValueNameParameterSetter {

    @Override
    protected void doSet(Request request, String paramName, Object paramValue) {
        request.setHttpVersion((Version) paramValue);
    }
}
