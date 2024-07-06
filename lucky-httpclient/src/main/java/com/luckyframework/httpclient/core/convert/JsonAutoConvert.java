package com.luckyframework.httpclient.core.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;

import java.lang.reflect.Type;

/**
 * JSON响应数据自动转换器
 */
public class JsonAutoConvert implements Response.AutoConvert {
    @Override
    public boolean can(Response resp, Type type) {
        try {
            // 返回值为类型为【不能自动关闭资源的类型】时不做处理
            if (HttpClientProxyObjectFactory.getNotAutoCloseResourceTypes().contains(type)) {
                return false;
            }
            if (resp.isJsonType()) {
                return true;
            }
            JsonElement jsonElement = JsonParser.parseString(resp.getStringResult());
            return !jsonElement.isJsonNull();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public <T> T convert(Response resp, Type type) {
        return resp.jsonStrToEntity(type);
    }
}
