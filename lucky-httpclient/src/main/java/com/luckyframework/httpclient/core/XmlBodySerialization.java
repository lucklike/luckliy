package com.luckyframework.httpclient.core;

import static com.luckyframework.httpclient.core.SerializationConstant.XML_SCHEME;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 11:25
 */
public class XmlBodySerialization extends StringBodySerialization {

    @Override
    protected String serializationToString(Object object) throws Exception {
        if (object instanceof String) {
            return (String) object;
        }
        return XML_SCHEME.serialization(object);
    }

}
