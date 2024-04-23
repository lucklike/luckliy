package com.luckyframework.httpclient.core;

import com.google.protobuf.Parser;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationException;

import java.lang.reflect.Type;

/**
 * Protobuf自动转换器
 */

public class ProtobufAutoConvert implements Response.AutoConvert {
    @Override
    public boolean can(Response resp) {
        return resp.getContentType().getMimeType().equalsIgnoreCase(ContentType.APPLICATION_PROTOBUF.getMimeType());
    }


    @Override
    public <T> T convert(Response resp, Type type) {
        try {
            @SuppressWarnings("unchecked")
            Parser<T> parser = (Parser<T>) MethodUtils.invokeDeclaredMethod(type, "parser");
            return parser.parseFrom(resp.getResult());
        } catch (Exception e) {
            throw new SerializationException(e, "It is not possible to convert the response to a Protobuf object.");
        }
    }
}
