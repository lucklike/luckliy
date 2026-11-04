package com.luckyframework.httpclient.generalapi.ai;

import com.luckyframework.httpclient.core.serialization.SerializationConstant;
import com.luckyframework.serializable.JsonSerializationScheme;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationTypeToken;

import java.lang.reflect.Type;

/**
 * AI模块内部的JSON工具，统一走框架的JSON_SCHEME（默认Jackson，
 * 已全局配置「忽略未知字段」与「null字段省略」两条前提）。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，不属于对外承诺的公开接口
 */
public final class AiJsonUtils {

    private AiJsonUtils() {
    }

    private static JsonSerializationScheme scheme() {
        return SerializationConstant.JSON_SCHEME;
    }

    /**
     * 对象转JSON文本
     */
    public static String toJson(Object object) {
        try {
            return scheme().serialization(object);
        } catch (Exception e) {
            throw new SerializationException(e, "Description Failed to serialize the AI model to JSON.");
        }
    }

    /**
     * JSON文本转对象
     */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return type.cast(scheme().deserialization(json, (Type) type));
        } catch (Exception e) {
            throw new SerializationException(e, "Description Failed to deserialize the JSON to " + type.getName() + ".");
        }
    }

    /**
     * JSON文本转带泛型的对象
     */
    public static <T> T fromJson(String json, SerializationTypeToken<T> typeToken) {
        try {
            return scheme().deserialization(json, typeToken);
        } catch (Exception e) {
            throw new SerializationException(e, "Description Failed to deserialize the JSON to " + typeToken.getType() + ".");
        }
    }
}
