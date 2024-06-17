package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.RefType;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

/**
 * Ref参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/5/7 21:44
 */
public class RefParameterSetter implements ParameterSetter{

    @Override
    public void set(Request request, ParamInfo paramInfo) {
        Object name = paramInfo.getName();
        String ref = String.valueOf(paramInfo.getValue());
        if (name == RefType.SET) {
            request.setRef(ref);
        } else {
            String oldRef = request.getRef();
            request.setRef(oldRef == null ? ref : oldRef + ref);
        }
    }
}
