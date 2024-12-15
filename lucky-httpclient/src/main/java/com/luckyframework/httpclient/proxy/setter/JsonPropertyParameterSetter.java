package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.BodyObjectFactory;
import com.luckyframework.httpclient.core.meta.ConfigurationMapBodyObjectFactory;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import static com.luckyframework.httpclient.core.meta.ContentType.APPLICATION_JSON;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * Json属性参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/13 02:40
 */
public class JsonPropertyParameterSetter implements ParameterSetter {

    private final String dateKey;

    public JsonPropertyParameterSetter(String dateKey) {
        this.dateKey = dateKey;
    }

    public JsonPropertyParameterSetter() {
        this(null);
    }


    /**
     * 将参数设置到工厂类中
     *
     * @param request   请求体
     * @param paramInfo 参数信息
     */
    @Override
    public void set(Request request, ParamInfo paramInfo) {
        ConfigurationMapBodyObjectFactory jsonConfigurationMapBodyObjectFactory = getJsonMapBodyObjectFactory(request);
        jsonConfigurationMapBodyObjectFactory.addProperty(String.valueOf(paramInfo.getName()), paramInfo.getValue());
    }

    /**
     * 获取请求体参数工厂
     *
     * @param request 请求实例
     * @return 请求体参数工厂
     */
    private synchronized ConfigurationMapBodyObjectFactory getJsonMapBodyObjectFactory(Request request) {
        BodyObjectFactory bodyFactory = request.getBodyFactory();
        if (!(bodyFactory instanceof ConfigurationMapBodyObjectFactory)) {
            bodyFactory = ConfigurationMapBodyObjectFactory.of(dateKey, JSON_SCHEME, APPLICATION_JSON);
            request.setContentType(APPLICATION_JSON);
            request.setBodyFactory(bodyFactory);
        }
        return (ConfigurationMapBodyObjectFactory) bodyFactory;
    }
}
