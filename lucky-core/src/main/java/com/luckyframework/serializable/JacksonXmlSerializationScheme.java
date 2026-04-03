package com.luckyframework.serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import java.lang.reflect.Type;

public class JacksonXmlSerializationScheme implements XmlSerializationScheme {

    private static final XmlMapper xmlMapper = new XmlMapper();

    static {
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
    }

    @Override
    public String serialization(Object object) throws Exception {
        return xmlMapper.writeValueAsString(object);
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        return xmlMapper.readValue(objectStr, new  TypeReference<Object>() {
            @Override
            public Type getType() {
                return objectType;
            }
        });
    }
}
