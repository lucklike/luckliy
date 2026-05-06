package com.luckyframework.serializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;

/**
 * JDK序列化方案
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 7:04 下午
 */
public class JDKSerializationScheme implements SerializationScheme {

    @Override
    public String serialization(Object object) throws IOException {
        return new String(toByte(object));
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        return fromByte(objectStr.getBytes());
    }

    public byte[] toByte(Object object) throws IOException {
        try (ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
             ObjectOutputStream objOut = new ObjectOutputStream(byteArrayOut)) {
            objOut.writeObject(object);
            return byteArrayOut.toByteArray();
        }
    }

    public Object fromByte(byte[] bytes) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return objectInputStream.readObject();
        }
    }
}
