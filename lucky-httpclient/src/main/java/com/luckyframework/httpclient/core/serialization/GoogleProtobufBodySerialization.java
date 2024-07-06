package com.luckyframework.httpclient.core.serialization;

import com.google.protobuf.Message;
import com.luckyframework.serializable.SerializationException;

import java.nio.charset.Charset;

/**
 * Protobuf请求体序列化实现类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/23 14:09
 */
public class GoogleProtobufBodySerialization implements BodySerialization {

    @Override
    public byte[] serialization(Object object, Charset charset) throws Exception {
        Class<?> aClass = object.getClass();
        if (Message.class.isAssignableFrom(aClass)) {
            return ((Message) object).toByteArray();
        }
        throw new SerializationException("Serialization Exception: '" + aClass + "' is not a Protobuf message type");
    }
}
