package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.core.serialization.XmlBodySerialization;
import com.luckyframework.serializable.SerializationException;

/**
 * 静态Xml Body参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/24 10:46
 */
public class XmlBodyHandle extends BodyStaticParamResolver.StringBodyHandle {

    private final XmlBodySerialization xmlBodySerialization = new XmlBodySerialization();

    @Override
    protected String stringBody(Object body) {
        try {
            return xmlBodySerialization.serializationToString(body);
        }catch (Exception e){
            throw new SerializationException(e, "xml body serialization error!");
        }

    }
}
