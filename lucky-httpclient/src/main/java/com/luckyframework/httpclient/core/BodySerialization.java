package com.luckyframework.httpclient.core;

import java.nio.charset.Charset;

/**
 * 请求体序列化接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 11:21
 */
@FunctionalInterface
public interface BodySerialization {

    byte[] serialization(Object object, Charset charset) throws Exception;

}
