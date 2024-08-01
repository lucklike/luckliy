package com.luckyframework.serializable;

import org.springframework.util.FileCopyUtils;

import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Type;

/**
 * 序列化方案
 *
 * @author fk7075
 * @version 1.0
 * @date 2020/11/12 10:45
 */
@SuppressWarnings("unchecked")
public interface SerializationScheme {

    /**
     * 序列化，将一个Java对象序列化为字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的字符串
     * @throws Exception 序列化出现问题时会抛出该异常
     */
    String serialization(Object object) throws Exception;

    /**
     * 反序列化，讲一个字符串翻序列化为一个Java对象
     *
     * @param objectStr  待反序列化的字符串
     * @param objectType 序列化后Java对象的类型
     * @return 序列化之后的Java对象
     * @throws Exception 序列化出现问题时会触发该异常
     */
    Object deserialization(String objectStr, Type objectType) throws Exception;

    /**
     * 反序列化，讲一个字符串翻序列化为一个Java对象
     *
     * @param objectStr 待反序列化的字符串
     * @param typeToken 序列化后Java对象的类型的Token信息
     * @return 序列化之后的Java对象
     * @throws Exception 序列化出现问题时会触发该异常
     */
    default <T> T deserialization(String objectStr, SerializationTypeToken<T> typeToken) throws Exception {
        return (T) deserialization(objectStr, typeToken.getType());
    }

    /**
     * 反序列化，讲一个字符串翻序列化为一个Java对象
     *
     * @param reader    待反序列化的Reader
     * @param typeToken 序列化后Java对象的类型的Token信息
     * @return 序列化之后的Java对象
     * @throws Exception 序列化出现问题时会触发该异常
     */
    default <T> T deserialization(Reader reader, SerializationTypeToken<T> typeToken) throws Exception {
        return (T) deserialization(reader, typeToken.getType());
    }

    /**
     * 反序列化，讲一个字符串翻序列化为一个Java对象
     *
     * @param reader     待反序列化的Reader
     * @param objectType 序列化后Java对象的类型
     * @return 序列化之后的Java对象
     * @throws Exception 序列化出现问题时会触发该异常
     */
    default Object deserialization(Reader reader, Type objectType) throws Exception {
        StringWriter writer = new StringWriter();
        FileCopyUtils.copy(reader, writer);
        return deserialization(writer.toString(), objectType);
    }
}
