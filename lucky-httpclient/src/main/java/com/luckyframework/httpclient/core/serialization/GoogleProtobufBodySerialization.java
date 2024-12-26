package com.luckyframework.httpclient.core.serialization;

import com.google.protobuf.MessageLite;
import com.luckyframework.serializable.SerializationException;

import java.nio.charset.Charset;
import java.util.function.Supplier;

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
        if (MessageLite.class.isAssignableFrom(aClass)) {
            return ((MessageLite) object).toByteArray();
        }
        throw new SerializationException("Serialization Exception: '" + aClass + "' is not a Protobuf message type");
    }

    @Override
    public Supplier<String> stringSupplier(Object object, byte[] objBytes, String mimeType, Charset charset) {
        return () -> String.valueOf(object);
    }

}
