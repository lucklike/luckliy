package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.DefaultRequest;
import com.luckyframework.httpclient.core.meta.Request;

/**
 * Url符参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class UrlParameterSetter extends ValueNameParameterSetter {

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        if (request instanceof DefaultRequest){
            DefaultRequest defaultRequest = (DefaultRequest) request;
            String newUrlTemp = StringUtils.joinUrlPath(defaultRequest.getUrlTemplate(), String.valueOf(paramValue));
            newUrlTemp = StringUtils.format(newUrlTemp, request.getPathParameters());
            defaultRequest.setUrlTemplate(newUrlTemp);
        }
    }
}
