package com.luckyframework.httpclient.core.serialization;


import java.nio.charset.Charset;
import java.util.function.Supplier;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;

/**
 * Java序列化对象请求体序列化实现类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/11 16:48
 */
public class JavaObjectBodySerialization implements BodySerialization {

    @Override
    public byte[] serialization(Object object, Charset charset) throws Exception {
        return JDK_SCHEME.toByte(object);
    }

    @Override
    public Supplier<String> stringSupplier(Object object, byte[] objBytes, String mimeType, Charset charset) {
        return () -> String.valueOf(object);
    }

}
