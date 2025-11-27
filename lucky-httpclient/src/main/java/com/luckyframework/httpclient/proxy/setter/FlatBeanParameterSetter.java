package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.common.FlatBean;
import com.luckyframework.httpclient.core.meta.FlatBeanBodyObjectFactory;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

public class FlatBeanParameterSetter implements ParameterSetter {

    @Override
    public void set(Request request, ParamInfo paramInfo) {
        FlatBeanBodyObjectFactory.forJsonRequest(request, FlatBean.of(paramInfo.getValue()));
    }
}
