package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ConfigurationMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Properties;

import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JDK_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.XML_SCHEME;

/**
 * 编码工具类
 */
public class EncoderUtils {

    /**
     * base64编码
     *
     * @param str 待编码的字符串
     * @return 编码后的字符串
     */
    public static String base64(String str) {
        byte[] encode = Base64.getEncoder().encode(str.getBytes());
        return new String(encode);
    }

    /**
     * 构建BasicAut格式的字符串
     *
     * @param username 用户名
     * @param password 密码
     * @return BasicAut格式的字符串
     */
    public static String basicAuth(String username, String password) {
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
