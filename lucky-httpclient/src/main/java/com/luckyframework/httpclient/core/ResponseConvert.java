package com.luckyframework.httpclient.core;

import java.lang.reflect.Type;

/**
 * 响应体转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 10:18
 */
@FunctionalInterface
public interface ResponseConvert {

    <T> T convert(Response response, Type resultType) throws Exception;
}
