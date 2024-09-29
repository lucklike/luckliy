package com.luckyframework.httpclient.proxy.logging.processor;

import com.luckyframework.serializable.JaxbXmlSerializationScheme;

public class XmlBodyProcessor extends AbstractBodyProcessor {

    @Override
    public String process(String body) {
        try {
            String xml = JaxbXmlSerializationScheme.prettyPrintByTransformer(body);
            return xml.replace("\n", "\n" + translation);
        } catch (Exception e) {
            return body;
        }
    }

}
