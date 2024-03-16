package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.BinaryBodySerialization;

import java.nio.charset.Charset;

/**
 * 静态JSON Body参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/19 10:50
 */
public class BinaryBodyHandle implements BodyStaticParamResolver.BodyHandle {

    @Override
    public byte[] handle(Object body, Charset charset) {
        try {
            return new BinaryBodySerialization().serialization(body, charset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}