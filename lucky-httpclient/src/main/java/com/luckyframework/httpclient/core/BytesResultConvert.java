package com.luckyframework.httpclient.core;

import java.lang.reflect.Type;

/**
 * byte[]类型响应结果转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 10:18
 */
@FunctionalInterface
public interface BytesResultConvert {

    <T> T convert(byte[] bytesResult, Type resultType) throws Exception;

}
