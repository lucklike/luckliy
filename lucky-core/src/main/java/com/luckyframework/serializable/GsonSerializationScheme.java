package com.luckyframework.serializable;

import com.google.gson.Gson;

import java.lang.reflect.Type;

/**
 * Google的Gson持久化方案
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 7:06 下午
 */
public class GsonSerializationScheme implements JsonSerializationScheme{

    private final static Gson gson = new Gson();

    @Override
    public String serialization(Object object) throws Exception {
        return gson.toJson(object);
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        return gson.fromJson(objectStr, objectType);
    }
}
