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

    @Override
    protected String stringBody(Object body) {
        try {
            return new XmlBodySerialization().serializationToString(body);
        }catch (Exception e){
            throw new SerializationException(e, "xml body serialization error!");
        }

    }
}
