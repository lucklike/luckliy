package com.luckyframework.httpclient.core.serialization;

import org.springframework.util.Assert;

import java.nio.charset.Charset;

/**
 * 字符类型请求体序列化抽象类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/11 16:33
 */
public abstract class StringBodySerialization implements BodySerialization {

    @Override
    public byte[] serialization(Object object, Charset charset) throws Exception {
        Assert.notNull(charset, "Charset is null");
        return serializationToString(object).getBytes(charset);
    }

    protected abstract String serializationToString(Object object) throws Exception;
}
