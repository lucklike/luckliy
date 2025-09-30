package com.luckyframework.httpclient.core.convert;

import com.google.protobuf.Parser;
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
        return resp.isProtobufBody();
    }

    @Override
    public <T> T convert(Response resp, Type type) {
        return convertProtobuf(resp, type);
    }


    /**
     * 将响应对象转换为Protobuf消息对象
     *
     * @param response 响应体
     * @param type     转换类型
     * @param <T>      Protobuf消息对象类型
     * @return 对应的Protobuf消息对象
     */
    public static <T> T convertProtobuf(Response response, Type type) {
        try {
            @SuppressWarnings("unchecked")
            Parser<T> parser = (Parser<T>) MethodUtils.invokeDeclaredMethod(type, "parser");
            return parser.parseFrom(response.getResult());
        } catch (Exception e) {
            throw new SerializationException(e, "It is not possible to convert the response to a Protobuf object.");
        }
    }
}
