package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.impl.DefaultRequest;
import com.luckyframework.httpclient.proxy.ParameterSetter;

/**
 * Url符参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class UrlParameterSetter implements ParameterSetter {

    @Override
    public void set(Request request, String paramName, Object paramValue) {
        if (request instanceof DefaultRequest){
            DefaultRequest defaultRequest = (DefaultRequest) request;
            String newUrlTemp = StringUtils.joinUrlPath(defaultRequest.getUrlTemplate(), String.valueOf(paramValue));
            defaultRequest.setUrlTemplate(newUrlTemp);
        }
    }
}
