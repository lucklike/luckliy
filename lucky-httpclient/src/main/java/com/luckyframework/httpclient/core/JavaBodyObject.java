package com.luckyframework.httpclient.core;

import java.io.IOException;
import java.io.Serializable;

import static com.luckyframework.httpclient.core.SerializationConstant.JDK_SCHEME;

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
