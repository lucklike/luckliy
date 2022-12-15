package com.luckyframework.serializable;

/**
 * XML序列化方案
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 7:06 下午
 */
public interface XmlSerializationScheme extends SerializationScheme{

    String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
}
