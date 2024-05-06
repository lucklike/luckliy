package com.luckyframework.httpclient.core;

import com.google.gson.JsonParser;
import com.luckyframework.serializable.SerializationException;

import java.lang.reflect.Type;

import static com.luckyframework.httpclient.core.SerializationConstant.JSON_SCHEME;

public class JsonAutoConvert implements Response.AutoConvert {
    @Override
    public boolean can(Response resp) {
        try {
            JsonParser.parseString(resp.getStringResult());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convert(Response resp, Type type) {
        try {
            return (T) JSON_SCHEME.deserialization(resp.getStringResult(), type);
        } catch (Exception e) {
            throw new SerializationException(e, "json deserialization error: " + resp.getStringResult());
        }
    }
}
