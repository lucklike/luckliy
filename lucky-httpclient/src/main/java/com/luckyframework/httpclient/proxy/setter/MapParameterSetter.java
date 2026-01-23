package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.ConfigurationMapBodyObjectFactory;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Map;


/**
 * Map响应体参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/13 02:40
 */
public class MapParameterSetter implements ParameterSetter {

    @Override
    @SuppressWarnings("unchecked")
    public void set(Request request, ParamInfo paramInfo) {
        String dataKey = (String) paramInfo.getName();
        Map<String, Object> configMap = ( Map<String, Object>) paramInfo.getValue();
        ConfigurationMapBodyObjectFactory.forRequest(request, configMap, dataKey);
    }

}
