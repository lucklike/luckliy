package com.luckyframework.httpclient.proxy.paraminfo;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;

/**
 * 静态参数信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 00:33
 */
public class CarrySetterParamInfo extends ParamInfo {

    private final ParameterSetter defaultSetter;

    public CarrySetterParamInfo(Object name, Object value, ParameterSetter defaultSetter) {
        super(name, value);
        this.defaultSetter = defaultSetter;
    }

    public CarrySetterParamInfo(ParamInfo paramInfo, ParameterSetter defaultSetter) {
        this(paramInfo.getName(), paramInfo.getValue(), defaultSetter);
    }

    public ParameterSetter getDefaultSetter() {
        return defaultSetter;
    }

    public void setParameter(Request request) {
        defaultSetter.set(request, this);
    }
}
