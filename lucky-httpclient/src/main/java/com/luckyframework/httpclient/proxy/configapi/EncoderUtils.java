package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.*;

/**
 * 编码工具类
 */
public class EncoderUtils {

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
     * </pre>
     *
     * @param object 待编码的内容
     * @return 编码后的字符串
     */
    public static String base64(Object object) throws IOException {
        byte[] encode;
        if (object instanceof String) {
            encode = ((String) object).getBytes(StandardCharsets.UTF_8);
        } else if (object instanceof byte[]) {
            encode = (byte[]) object;
        } else if (object instanceof InputStream) {
            encode = FileCopyUtils.copyToByteArray((InputStream) object);
        } else if (object instanceof File) {
            encode = FileCopyUtils.copyToByteArray((File) object);
        } else if (object instanceof InputStreamSource) {
            encode = FileCopyUtils.copyToByteArray(((InputStreamSource) object).getInputStream());
        } else if (object instanceof Reader) {
            encode = FileCopyUtils.copyToString((Reader) object).getBytes(StandardCharsets.UTF_8);
        }else {
            throw new SerializationException("base64 encoded object types are not supported: {}", object == null ? "null" : object.getClass());
        }
        return new String(Base64.getEncoder().encode(encode));
    }

    /**
     * 构建BasicAut格式的字符串
     *
     * @param username 用户名
     * @param password 密码
     * @return BasicAut格式的字符串
     */
    public static String basicAuth(String username, String password) throws IOException {
        String auth = "Basic " + username + ":" + password;
        return base64(auth);
    }

    /**
     * 将字符串按照指定字符集进行url编码
     *
     * @param str     待编码的字符串
     * @param charset 编码格式
     * @return 编码后的字符串
     * @throws UnsupportedEncodingException 编码过程中可能出现的异常
     */
    public static String urlCharset(String str, String charset) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, charset);
    }

    /**
     * 将字符串按照UTF-8字符集进行url编码
     *
     * @param str 待编码的字符串
     * @return 编码后的字符串
     * @throws UnsupportedEncodingException 编码过程中可能出现的异常
     */
    public static String url(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    /**
     *【英文小写】 md5加密
     * <pre>
     *  支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     * </pre>
     *
     * @param object 待加密的内容
     * @return 编码后的字符串
     * @throws IOException 加密过程中可能出现的异常
     */
    public static String md5(Object object) throws IOException {
        if (object instanceof byte[]) {
            return DigestUtils.md5DigestAsHex((byte[]) object);
        }
        if (object instanceof String) {
            return DigestUtils.md5DigestAsHex(((String) object).getBytes(StandardCharsets.UTF_8));
        }
        if (object instanceof InputStream) {
            return DigestUtils.md5DigestAsHex((InputStream) object);
        }
        if (object instanceof InputStreamSource) {
            return DigestUtils.md5DigestAsHex(((InputStreamSource) object).getInputStream());
        }
        if (object instanceof Reader) {
            return DigestUtils.md5DigestAsHex(FileCopyUtils.copyToString((Reader) object).getBytes(StandardCharsets.UTF_8));
        }
        if (object instanceof File) {
            return DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray((File) object));
        }
        throw new SerializationException("md5 encipher object types are not supported: {}", object == null ? "null" : object.getClass());
    }

    /**
     * 【英文大写】 md5加密
     * <pre>
     *  支持的入参类型有：
     *     1.{@link String}
     *     2.{@link byte[]}
     *     3.{@link InputStream}
     *     4.{@link InputStreamSource}
     *     5.{@link Reader}
     *     6.{@link File}
     * </pre>
     *
     * @param object 待加密的内容
     * @return 编码后的字符串
     * @throws IOException 加密过程中可能出现的异常
     */
    public static String MD5(Object object) throws IOException {
        return md5(object).toUpperCase();
    }

    /**
     * 将对象序列化为json字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的json字符串
     * @throws Exception 序列化过程中可能出现的异常
     */
    public static String json(Object object) throws Exception {
        return JSON_SCHEME.serialization(object);
    }

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

    /**
     * 将对象序列化为jdk序列化字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的jdk序列化字符串
     * @throws IOException 序列化过程中可能出现的异常
     */
    public static String java(Object object) throws IOException {
        return JDK_SCHEME.serialization(object);
    }

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

    /**
     * 将字符串转换为Resource对象
     *
     * @param path 资源路径
     * @return 资源对象
     */
    public static Resource resource(String path) {
        return ConversionUtils.conversion(path, Resource.class);
    }

    /**
     * 将字符串转数组换为Resource对象数组
     *
     * @param paths 资源路径数组
     * @return 资源对象数组
     */
    public static Resource[] resources(String... paths) {
        List<Resource> resources = new ArrayList<>();
        for (String path : paths) {
            resources.addAll(Arrays.asList(ConversionUtils.conversion(path, Resource[].class)));
        }
        return resources.toArray(new Resource[0]);
    }

}
