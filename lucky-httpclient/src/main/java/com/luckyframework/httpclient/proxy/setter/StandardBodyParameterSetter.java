package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.meta.Request;

/**
 * 标准请求体参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/1 04:27
 */
public class StandardBodyParameterSetter extends ValueNameParameterSetter {
    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        request.setBody(((BodyObject) paramValue));
    }
}
