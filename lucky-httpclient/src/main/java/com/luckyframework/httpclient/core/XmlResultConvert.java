package com.luckyframework.httpclient.core;

import com.luckyframework.serializable.SerializationSchemeFactory;

import java.lang.reflect.Type;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/17 23:23
 */
@SuppressWarnings("unchecked")
public class XmlResultConvert implements StringResultConvert{
    @Override
    public <T> T convert(String stringResult, Type resultType) throws Exception {
        return (T) SerializationSchemeFactory.getXmlScheme().deserialization(stringResult, resultType);
    }
}
