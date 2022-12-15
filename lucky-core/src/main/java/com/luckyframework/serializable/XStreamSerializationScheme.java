package com.luckyframework.serializable;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;

import java.lang.reflect.Type;

/**
 * Xtream的XML序列化方案
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 7:11 下午
 */
public class XStreamSerializationScheme implements XmlSerializationScheme{

    private final static XStream xstream;

    static {
        xstream = new XStream(new XppDriver(new XmlFriendlyNameCoder("_-", "_")));
        xstream.autodetectAnnotations(true);
        XStream.setupDefaultSecurity(xstream);
    }

    @Override
    public String serialization(Object object) throws Exception {
        String xmlStr = xstream.toXML(object);
        xmlStr = xmlStr.replace("&quot;", "\"");
        return HEAD + xmlStr;
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        xstream.addPermission(AnyTypePermission.ANY);
        return xstream.fromXML(objectStr);
    }
}
