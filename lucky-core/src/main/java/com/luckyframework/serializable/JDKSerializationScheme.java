package com.luckyframework.serializable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

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
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(byteArrayOut);
        objOut.writeObject(object);
        String objectStr = byteArrayOut.toString("ISO-8859-1");
        objOut.close();
        byteArrayOut.close();
        return objectStr;
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(objectStr.getBytes(StandardCharsets.ISO_8859_1));
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        Object object = objectInputStream.readObject();
        objectInputStream.close();
        byteArrayInputStream.close();
        return object;
    }

    public byte[] toByte(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(byteArrayOut);
        objOut.writeObject(object);
        objOut.close();
        byteArrayOut.close();
        return byteArrayOut.toByteArray();
    }

    public Object fromByte(byte[] bytes) throws Exception {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        Object object = objectInputStream.readObject();
        objectInputStream.close();
        byteArrayInputStream.close();
        return object;
    }
}
