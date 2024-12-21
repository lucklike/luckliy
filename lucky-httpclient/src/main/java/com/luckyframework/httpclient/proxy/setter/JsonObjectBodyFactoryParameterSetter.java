package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.List;

/**
 * 基于{@link JsonObjectBodyFactoryParameterSetter}实现的多参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/16 00:36
 */
public class JsonObjectBodyFactoryParameterSetter implements ParameterSetter {

    private final JsonPropertyParameterSetter jsonPropertyParameterSetter = new JsonPropertyParameterSetter();

    @Override
    @SuppressWarnings("unchecked")
    public void set(Request request, ParamInfo paramInfo) {
        List<ParamInfo> paramInfoList = (List<ParamInfo>) paramInfo.getValue();
        for (ParamInfo info : paramInfoList) {
            jsonPropertyParameterSetter.set(request, info);
        }
    }
}
