package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

public class NonParameterSetter implements ParameterSetter{

    @Override
    public void set(Request request, ParamInfo paramInfo) {
        // 不做任何事情
    }
}
