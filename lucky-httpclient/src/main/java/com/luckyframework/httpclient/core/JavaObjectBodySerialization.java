package com.luckyframework.httpclient.core;


import java.nio.charset.Charset;

import static com.luckyframework.httpclient.core.SerializationConstant.JDK_SCHEME;

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
}
