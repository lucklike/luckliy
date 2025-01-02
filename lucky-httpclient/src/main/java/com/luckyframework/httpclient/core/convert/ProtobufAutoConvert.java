package com.luckyframework.httpclient.core.convert;

import com.google.protobuf.Parser;
import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationException;

import java.lang.reflect.Type;

/**
 * Protobuf自动转换器
 */
public class ProtobufAutoConvert implements Response.AutoConvert {

    @Override
    public boolean can(Response resp, Type type) {
        return ContentType.APPLICATION_PROTOBUF.getMimeType().equalsIgnoreCase(resp.getContentType().getMimeType());
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
