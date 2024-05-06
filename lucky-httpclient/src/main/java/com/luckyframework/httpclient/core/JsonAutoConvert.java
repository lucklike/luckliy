package com.luckyframework.httpclient.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;

public class JsonAutoConvert implements Response.AutoConvert {
    @Override
    public boolean can(Response resp) {
        try {
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
