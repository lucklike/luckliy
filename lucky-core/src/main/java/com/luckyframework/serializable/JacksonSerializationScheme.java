package com.luckyframework.serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

/**
 * Jackson序列化方案
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/7 00:04
 */
public class JacksonSerializationScheme implements JsonSerializationScheme {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String serialization(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        return objectMapper.readValue(objectStr, new TypeReference<Object>() {
            @Override
            public Type getType() {
                return objectType;
            }
        });
    }

    /**
     * Json字符串格式化
     * @param jsonString json字符串
     * @return 格式化后的json字符串
     */
    public static String prettyPrinting(String jsonString) {
        try {
            Object jsonObject = objectMapper.readValue(jsonString, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        }catch (Exception e){
            throw new SerializationException(e, "Description Failed to format the Json string.");
        }
    }
}
