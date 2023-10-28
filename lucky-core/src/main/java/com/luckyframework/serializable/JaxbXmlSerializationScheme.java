package com.luckyframework.serializable;

import org.springframework.core.ResolvableType;

import javax.xml.bind.JAXB;
import java.io.StringWriter;
import java.lang.reflect.Type;

/**
 * 基于JDK JAXB的XML序列化方案
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/29 00:27
 */
public class JaxbXmlSerializationScheme implements XmlSerializationScheme{
    @Override
    public String serialization(Object object) throws Exception {
        StringWriter sw = new StringWriter();
        JAXB.marshal(object, sw);
        return sw.toString();
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        return JAXB.unmarshal(objectStr, ResolvableType.forType(objectType).resolve());
    }
}
