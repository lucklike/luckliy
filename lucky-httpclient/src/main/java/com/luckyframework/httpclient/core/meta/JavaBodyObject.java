package com.luckyframework.httpclient.core.meta;

import java.io.IOException;
import java.io.Serializable;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;

/**
 * Java序列化对象Body
 *
 * @author fk7075
 * @version 1.0.
 * @date 2024/6/24 13:36
 * @param <T> Java序列化对象类型
 */
@SuppressWarnings("unchecked")
public class JavaBodyObject<T extends Serializable> extends BodyObject {

    private final Class<T> type;

    JavaBodyObject(T serializable) throws IOException {
        super(ContentType.APPLICATION_JAVA_SERIALIZED_OBJECT, JDK_SCHEME.toByte(serializable));
        this.type = (Class<T>) serializable.getClass();
    }

    public Class<?> getType() {
        return type;
    }

    public  T getJavaObject() throws Exception {
        return (T) JDK_SCHEME.fromByte(getBody());
    }
}
