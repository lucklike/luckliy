package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.ConfigurationMapBodyObjectFactory;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

/**
 * Json属性参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/13 02:40
 */
public class JsonPropertyParameterSetter implements ParameterSetter {


    /**
     * 将参数设置到工厂类中
     *
     * @param request   请求体
     * @param paramInfo 参数信息
     */
    @Override
    public void set(Request request, ParamInfo paramInfo) {
        ConfigurationMapBodyObjectFactory jsonConfigurationMapBodyObjectFactory = ConfigurationMapBodyObjectFactory.forRequest(request, null);
        if (paramInfo.getValue() != null) {
            jsonConfigurationMapBodyObjectFactory.addProperty(String.valueOf(paramInfo.getName()), paramInfo.getValue());
        }
    }
}
