package com.luckyframework.httpclient.core.serialization;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * Json格式请求体参数序列化方案
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 11:24
 */
public class JsonBodySerialization extends StringBodySerialization {

    @Override
    public String serializationToString(Object object) throws Exception {
        if (object instanceof String) {
            return (String) object;
        }
        return JSON_SCHEME.serialization(object);
    }

}
