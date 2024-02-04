package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.RequestMethod;

/**
 * 请求方法设置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 00:21
 */
public class RequestMethodSetter extends ValueNameParameterSetter {

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        request.setRequestMethod(((RequestMethod) paramValue));
    }
}
