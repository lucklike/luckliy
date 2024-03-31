package com.luckyframework.serializable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;

/**
 * Google的Gson持久化方案
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 7:06 下午
 */
public class GsonSerializationScheme implements JsonSerializationScheme {

    private final static Gson gson = new Gson();

    @Override
    public String serialization(Object object) throws Exception {
        return gson.toJson(object);
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        return gson.fromJson(objectStr, objectType);
    }

    /**
     * Json字符串格式化
     * @param jsonString json字符串
     * @return 格式化后的json字符串
     */
    public static String prettyPrinting(String jsonString) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = JsonParser.parseString(jsonString);
        return gson.toJson(je);
    }
}
