package com.luckyframework.httpclient.proxy.impl.setter;

import com.luckyframework.httpclient.core.Request;

/**
 * Basic Auth 参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:21
 */
public class BasicAuthParameterSetter extends ValueNameParameterSetter {

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        request.setAuthorization(paramName, String.valueOf(paramValue));
    }
}
