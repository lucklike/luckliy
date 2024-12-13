package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.BodyObjectFactory;
import com.luckyframework.httpclient.core.meta.MapBodyObjectFactory;
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
public class JsonFieldParameterSetter implements ParameterSetter {

    /**
     * 将参数设置到工厂类中
     *
     * @param request   请求体
     * @param paramInfo 参数信息
     */
    @Override
    public void set(Request request, ParamInfo paramInfo) {
        MapBodyObjectFactory jsonMapBodyObjectFactory = getJsonMapBodyObjectFactory(request);
        jsonMapBodyObjectFactory.addProperty(String.valueOf(paramInfo.getName()), paramInfo.getValue());
    }

    /**
     * 获取请求体参数工厂
     *
     * @param request 请求实例
     * @return 请求体参数工厂
     */
    private MapBodyObjectFactory getJsonMapBodyObjectFactory(Request request) {
        BodyObjectFactory bodyFactory = request.getBodyFactory();
        if (bodyFactory == null) {
            bodyFactory = MapBodyObjectFactory.create(JSON_SCHEME, APPLICATION_JSON);
            request.setContentType(APPLICATION_JSON);
            request.setBodyFactory(bodyFactory);
        }
        return (MapBodyObjectFactory) bodyFactory;
    }
}
