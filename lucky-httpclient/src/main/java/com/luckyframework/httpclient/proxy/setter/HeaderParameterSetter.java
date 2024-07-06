package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REMOVE_TAG;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.SET_TAG;

/**
 * 请求头参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class HeaderParameterSetter extends ValueNameParameterSetter {

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        if (paramName.startsWith(SET_TAG)) {
            request.setHeader(paramName.substring(SET_TAG.length()).trim(), paramValue);
        } else if (paramName.startsWith(REMOVE_TAG)) {
            request.removerHeader(paramName.substring(REMOVE_TAG.length()).trim());
        } else {
            request.addHeader(paramName, paramValue);
        }
    }
}
