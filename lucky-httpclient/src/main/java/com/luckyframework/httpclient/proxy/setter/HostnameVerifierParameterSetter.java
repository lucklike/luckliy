package com.luckyframework.httpclient.proxy.setter;


import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import javax.net.ssl.HostnameVerifier;

/**
 * HostnameVerifier 参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:21
 */
public class HostnameVerifierParameterSetter implements ParameterSetter {

    @Override
    public void set(Request request, ParamInfo paramInfo) {
        request.setHostnameVerifier(((HostnameVerifier) paramInfo.getValue()));
    }
}
