package com.luckyframework.serializable;

import org.springframework.core.ResolvableType;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * 基于JDK JAXB的XML序列化方案
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/29 00:27
 */
public class JaxbXmlSerializationScheme implements XmlSerializationScheme {
    @Override
    public String serialization(Object object) throws Exception {
        return javaBeanToXml(object, "UTF-8");
    }

    @Override
    public Object deserialization(String objectStr, Type objectType) throws Exception {
        return xmlToJavaBean(objectStr, (Class<?>) objectType);
    }

    /**
     * JavaBean转换成xml.
     *
     * @param obj      JavaBean.
     * @param encoding 字符集.
     * @return XML数据.
     */
    public static String javaBeanToXml(Object obj, String encoding) throws Exception {
        JAXBContext context = JAXBContext.newInstance(obj.getClass());
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
        StringWriter writer = new StringWriter();
        marshaller.marshal(obj, writer);
        return writer.toString();
    }

    /**
     * Xml转换成JavaBean.
     *
     * @param xml   XML.
     * @param clazz JavaBean类对象.
     * @return JavaBean.
     */
    @SuppressWarnings("unchecked")
    public static <T> T xmlToJavaBean(String xml, Class<T> clazz) throws Exception {
        JAXBContext context;
        Unmarshaller unmarshaller;
        context = JAXBContext.newInstance(clazz);
        unmarshaller = context.createUnmarshaller();
        return (T) unmarshaller.unmarshal(new StringReader(xml));
    }

    public static String prettyPrintByTransformer(String xmlString) {
        return prettyPrintByTransformer(xmlString, 4, false);
    }

    /**
     * 格式化xml
     *
     * @param xmlString         xml内容
     * @param indent            向前缩进多少空格
     * @param ignoreDeclaration 是否忽略描述
     * @return 格式化后的xml
     */
    public static String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) {

        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString().replaceFirst("\\?>", "?>\n");
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }

}
