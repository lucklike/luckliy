package com.luckyframework.httpclient.core.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.luckyframework.httpclient.core.meta.Response;

import java.lang.reflect.Type;

/**
 * JSON响应数据自动转换器
 */
public class JsonAutoConvert implements Response.AutoConvert {
    @Override
    public boolean can(Response resp) {
        try {
            if (resp.isJsonType()){
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
