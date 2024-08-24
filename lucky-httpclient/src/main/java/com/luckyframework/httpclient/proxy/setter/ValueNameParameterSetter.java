package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

/**
 * 参数名-参数值设置
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/4 01:00
 */
public abstract class ValueNameParameterSetter implements ParameterSetter {
    @Override
    public void set(Request request, ParamInfo paramInfo) {
        Object value = paramInfo.getValue();
        if (value != null) {
            doSet(request, String.valueOf(paramInfo.getName()), value);
        }
    }

    protected abstract void doSet(Request request, String paramName, Object paramValue);
}
