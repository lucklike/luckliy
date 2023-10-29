package com.luckyframework.httpclient.proxy.impl.setter;

import com.luckyframework.httpclient.core.Request;

/**
 * 路径占位符参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class PathParameterSetter extends ValueNameParameterSetter {

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        request.addPathParameter(paramName, paramValue);
    }
}