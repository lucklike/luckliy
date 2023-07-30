package com.luckyframework.httpclient.core;

/**
 * 请求体序列化接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 11:21
 */
@FunctionalInterface
public interface BodySerialization {

    String serialization(Object object) throws Exception;

}
