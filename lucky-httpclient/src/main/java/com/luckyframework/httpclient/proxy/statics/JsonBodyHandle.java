package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.serialization.JsonBodySerialization;
import com.luckyframework.serializable.SerializationException;

/**
 * 静态JSON Body参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/19 10:50
 */
public class JsonBodyHandle extends BodyStaticParamResolver.StringBodyHandle {

    private final JsonBodySerialization jsonBodySerialization = new JsonBodySerialization();

    @Override
    protected String stringBody(Object body) {
        try {
            return jsonBodySerialization.serializationToString(body);
        } catch (Exception e) {
            throw new SerializationException(e, "json body serialization error!");
        }
    }
}
