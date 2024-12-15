package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.List;

/**
 * 基于{@link JsonArrayBodyFactoryParameterSetter}实现的多参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/16 00:36
 */
public class JsonArrayBodyFactoryParameterSetter implements ParameterSetter {

    private JsonPropertyParameterSetter jsonPropertyParameterSetter;

    @Override
    @SuppressWarnings("unchecked")
    public void set(Request request, ParamInfo paramInfo) {
        String dataKey = (String) paramInfo.getName();
        JsonPropertyParameterSetter jpps = getJsonPropertyParameterSetter(dataKey);
        List<ParamInfo> paramInfoList = (List<ParamInfo>) paramInfo.getValue();
        for (ParamInfo info : paramInfoList) {
            jpps.set(request, info);
        }
    }

    private synchronized JsonPropertyParameterSetter getJsonPropertyParameterSetter(String dataKey) {
        if (jsonPropertyParameterSetter == null) {
            jsonPropertyParameterSetter = new JsonPropertyParameterSetter(dataKey);
        }
        return jsonPropertyParameterSetter;
    }
}
