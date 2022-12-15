package com.luckyframework.serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 序列化方案工厂
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 7:16 下午
 */
public abstract class SerializationSchemeFactory {

    private final static Logger log = LoggerFactory.getLogger(SerializationSchemeFactory.class);

    private static JDKSerializationScheme jdkSerializationScheme;
    private static JsonSerializationScheme jsonSerializationScheme;
    private static XmlSerializationScheme xmlSerializationScheme;


    public static void setJdkScheme(JDKSerializationScheme jdkSerializationScheme) {
        SerializationSchemeFactory.jdkSerializationScheme = jdkSerializationScheme;
    }

    public static void setJsonScheme(JsonSerializationScheme jsonSerializationScheme) {
        SerializationSchemeFactory.jsonSerializationScheme = jsonSerializationScheme;
    }

    public static void setXmlScheme(XmlSerializationScheme xmlSerializationScheme) {
        SerializationSchemeFactory.xmlSerializationScheme = xmlSerializationScheme;
    }

    public static JDKSerializationScheme getJdkScheme(){
        if(jdkSerializationScheme == null){
            jdkSerializationScheme = new JDKSerializationScheme();
        }
        return jdkSerializationScheme;
    }

    public static JsonSerializationScheme getJsonScheme(){
        if(jsonSerializationScheme == null){
            jsonSerializationScheme = new GsonSerializationScheme();
            log.info("Using JSON decoding codec Google Gson");
        }
        return jsonSerializationScheme;
    }

    public static XmlSerializationScheme getXmlScheme(){
        if(xmlSerializationScheme == null){
            xmlSerializationScheme = new XStreamSerializationScheme();
            log.info("Using XML decoding codec XStreamXml");
        }
        return xmlSerializationScheme;
    }

}
