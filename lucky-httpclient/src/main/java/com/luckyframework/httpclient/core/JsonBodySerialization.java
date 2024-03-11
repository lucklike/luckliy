package com.luckyframework.httpclient.core;

import com.luckyframework.serializable.SerializationSchemeFactory;

/**
 * Json格式请求体参数序列化方案
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 11:24
 */
public class JsonBodySerialization extends StringBodySerialization {

    @Override
    protected String serializationToString(Object object) throws Exception {
        if (object instanceof String) {
            return (String) object;
        }
        return SerializationSchemeFactory.getJsonScheme().serialization(object);
    }

}
