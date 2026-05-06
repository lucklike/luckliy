package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.Namespace;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.XML_SCHEME;
import static com.luckyframework.httpclient.proxy.function.CommonFunctions.getCharset;
import static com.luckyframework.httpclient.proxy.function.ResourceFunctions.toInStream;
import static com.luckyframework.httpclient.proxy.spel.MethodSpaceConstant.SERIALIZATION_FUNCTION_SPACE;

/**
 * 序列化/编解码相关的函数
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/12/3 02:10
 */
@Namespace(SERIALIZATION_FUNCTION_SPACE)
public class SerializationFunctions {

    //---------------------------------------------------------------------------
    //                              Base64
    //---------------------------------------------------------------------------

    /**
     * base64编码
     * <pre>
     *     支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param object   待编码的内容
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 编码后的字符串
     */
    public static String base64(Object object, String... charsets) throws IOException {
        return new String(Base64.getEncoder().encode(FileCopyUtils.copyToByteArray(toInStream(object, charsets))), getCharset(charsets));
    }

    /**
     * base64解码
     * <pre>
     *     支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param object   base64编码之后的内容
     * @param charsets 如果需要指定编码格式，可以使用该参数
     * @return 解码后的字节数组
     */
    public static byte[] _base64(Object object, String... charsets) throws IOException {
        return Base64.getDecoder().decode(FileCopyUtils.copyToByteArray(toInStream(object, charsets)));
    }

    /**
     * base64解码为字符串
     * <pre>
     *     支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param object base64编码之后的内容
     * @return 解码后的字符串
     */
    @FunctionAlias("_base64_str")
    public static String _base64Str(Object object, String... charsets) throws IOException {
        return new String(_base64(object, charsets), getCharset(charsets));
    }

    //---------------------------------------------------------------------------
    //                                  Url
    //---------------------------------------------------------------------------


    /**
     * 将字符串按照知道个字符集进行url编码，不指定时默认使用UTF-8
     *
     * @param str     待编码的字符串
     * @param charset 字符集
     * @return 编码后的字符串
     * @throws UnsupportedEncodingException 编码过程中可能出现的异常
     */
    public static String url(String str, String... charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, getCharset(charset).name());
    }

    /**
     * 将字符串按照UTF-8字符集进行url解码
     *
     * @param str     待解码的字符串
     * @param charset 字符集
     * @return 解码后的字符串
     * @throws UnsupportedEncodingException 解码过程中可能出现的异常
     */
    public static String _url(String str, String... charset) throws UnsupportedEncodingException {
        return URLDecoder.decode(str, getCharset(charset).name());
    }

    //---------------------------------------------------------------------------
    //                                  Hex
    //---------------------------------------------------------------------------

    /**
     * 对数据进行16进制编码
     *
     * @param data 数据
     * @return 16进制编码后的数据
     */
    public static String hex(byte[] data) {
        return DatatypeConverter.printHexBinary(data).toLowerCase();
    }

    //---------------------------------------------------------------------------
    //                                  Json
    //---------------------------------------------------------------------------

    /**
     * 将对象序列化为JSON字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的json字符串
     * @throws Exception 序列化过程中可能出现的异常
     */
    public static String json(Object object) throws Exception {
        return JSON_SCHEME.serialization(object);
    }


    /**
     * 将JSON字符串转成对象
     * <pre>
     *  支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     *     7.{@link ByteBuffer}
     * </pre>
     *
     * @param json  JSON字符串
     * @param types 要转换的类型
     * @param <T>   转换目标对象类型
     * @return 转换后的对象
     * @throws Exception 转换过程中可能会有异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T _json(Object json, Object... types) throws Exception {
        Object typeInfo = ContainerUtils.isEmptyArray(types) ? Object.class : types[0];

        Type type;
        if (typeInfo instanceof Type) {
            type = (Type) typeInfo;
        } else if (typeInfo instanceof ResolvableType) {
            type = ((ResolvableType) typeInfo).getType();
        } else if (typeInfo instanceof SerializationTypeToken) {
            type = ((SerializationTypeToken<?>) typeInfo).getType();
        } else {
            throw new SerializationException("Conversion target type not supported by JSON parsing method： {}", typeInfo);
        }

        if (json instanceof String) {
            return (T) JSON_SCHEME.deserialization((String) json, type);
        }
        if (json instanceof InputStreamSource) {
            return (T) JSON_SCHEME.deserialization(new InputStreamReader(((InputStreamSource) json).getInputStream(), StandardCharsets.UTF_8), type);
        }
        if (json instanceof Reader) {
            return (T) JSON_SCHEME.deserialization((Reader) json, type);
        }
        if (json instanceof File) {
            return (T) JSON_SCHEME.deserialization(new FileReader((File) json), type);
        }
        if (json instanceof InputStream) {
            return (T) JSON_SCHEME.deserialization(new InputStreamReader(((InputStream) json), StandardCharsets.UTF_8), type);
        }
        if (json instanceof ByteBuffer) {
            return (T) JSON_SCHEME.deserialization(new String(((ByteBuffer) json).array(), StandardCharsets.UTF_8), type);
        }
        if (json instanceof byte[]) {
            return (T) JSON_SCHEME.deserialization(new String((byte[]) json, StandardCharsets.UTF_8), type);
        }
        throw new SerializationException("Serialized data types not supported by JSON parsing methods: {}", ClassUtils.getClassName(json));
    }


    //---------------------------------------------------------------------------
    //                                  Xml
    //---------------------------------------------------------------------------

    /**
     * 将对象序列化为xml字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的xml字符串
     * @throws Exception 序列化过程中可能出现的异常
     */
    public static String xml(Object object) throws Exception {
        return XML_SCHEME.serialization(object);
    }

    //---------------------------------------------------------------------------
    //                                  Java
    //---------------------------------------------------------------------------

    /**
     * 使用 JDK 序列化方法将对象序列化为二进制数据
     *
     * @param object 待序列化的对象
     * @return 序列化后的二进制数据
     * @throws IOException 序列化过程中可能出现的异常
     */
    public static byte[] java(Object object) throws IOException {
        return JDK_SCHEME.toByte(object);
    }

    //---------------------------------------------------------------------------
    //                                 protobuf
    //---------------------------------------------------------------------------


    /**
     * 将对象序列化为protobuf字节数组
     *
     * @param object 待序列化的对象
     * @return 序列化后的protobuf字节数组
     */
    public static byte[] protobuf(Object object) {
        if (object == null) {
            return new byte[0];
        }
        Class<?> objectClass = object.getClass();
        String messageClassName = "com.google.protobuf.MessageLite";
        Set<String> inheritanceStructure = ClassUtils.getInheritanceStructure(objectClass);
        if (inheritanceStructure.contains(messageClassName)) {
            return (byte[]) MethodUtils.invoke(object, "toByteArray");
        }

        throw new SerializationException("Serialization Exception: '" + objectClass + "' is not a Protobuf message type");
    }

    //---------------------------------------------------------------------------
    //                                 form
    //---------------------------------------------------------------------------

    /**
     * 将对象序列化为表单字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的表单字符串
     */
    public static String form(Object object) {
        if (object == null) {
            return "";
        }
        ConfigurationMap configMap = new ConfigurationMap(object);
        Properties properties = configMap.toProperties(true);
        StringBuilder formBodySb = new StringBuilder();
        Enumeration<?> enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            formBodySb.append(key).append("=").append(properties.getProperty(key)).append("&");
        }
        return formBodySb.substring(0, formBodySb.length() - 1);
    }
}
