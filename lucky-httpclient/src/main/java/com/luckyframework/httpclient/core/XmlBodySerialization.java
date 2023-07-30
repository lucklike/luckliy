package com.luckyframework.httpclient.core;

import com.luckyframework.serializable.SerializationSchemeFactory;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 11:25
 */
public class XmlBodySerialization implements BodySerialization {
    @Override
    public String serialization(Object object) throws Exception {
        if (object instanceof String) {
            return (String) object;
        }
        return SerializationSchemeFactory.getXmlScheme().serialization(object);
    }
}
