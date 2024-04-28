package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.Request;

/**
 * 请求头参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class HeaderParameterSetter extends ValueNameParameterSetter {

    private static final String SET_TAG = "[SET]";
    private static final String REMOVE_TAG = "[-]";

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        if (paramName.startsWith(SET_TAG)) {
            request.setHeader(paramName.substring(SET_TAG.length()).trim(), paramValue);
        } else if (paramName.startsWith(REMOVE_TAG)) {
            request.removerHeader(paramName);
        } else {
            request.addHeader(paramName, paramValue);
        }
    }
}
