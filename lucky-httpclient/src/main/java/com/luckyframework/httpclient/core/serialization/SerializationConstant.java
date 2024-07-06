package com.luckyframework.httpclient.core.serialization;

import com.luckyframework.serializable.JDKSerializationScheme;
import com.luckyframework.serializable.JsonSerializationScheme;
import com.luckyframework.serializable.SerializationSchemeFactory;
import com.luckyframework.serializable.XmlSerializationScheme;

/**
 * 序列化方案工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/11 16:48
 */
public class SerializationConstant {

    /**
     * Json序列化方案
     */
    public final static JsonSerializationScheme JSON_SCHEME = SerializationSchemeFactory.getJsonScheme();

    /**
     * XML序列化方案
     */
    public final static XmlSerializationScheme XML_SCHEME = SerializationSchemeFactory.getXmlScheme();

    /**
     * Java序列化方案
     */
    public final static JDKSerializationScheme JDK_SCHEME = SerializationSchemeFactory.getJdkScheme();


}
