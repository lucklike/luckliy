package com.luckyframework.httpclient.core;

import com.luckyframework.httpclient.exception.ResponseProcessException;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.serializable.JsonSerializationScheme;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationSchemeFactory;
import com.luckyframework.serializable.SerializationTypeToken;
import com.luckyframework.serializable.XmlSerializationScheme;
import org.springframework.core.io.InputStreamSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 响应接口
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/28 8:21 下午
 */
public interface Response {

    /**
     * Json序列化方案
     */
    JsonSerializationScheme jsonScheme = SerializationSchemeFactory.getJsonScheme();
    /**
     * XML序列化方案
     */
    XmlSerializationScheme xmlScheme = SerializationSchemeFactory.getXmlScheme();

    /**
     * 获取状态码
     */
    int getState();

    void setState(int state);

    /**
     * 获取响应头管理器
     */
    HttpHeaderManager getHeaderManager();

    void setHeaderManager(HttpHeaderManager headerManager);

    /**
     * 获取响应的Content-Type
     *
     * @return
     */
    default ContentType getContentType() {
        return getHeaderManager().getContentType();
    }

    void setResult(byte[] bytes);

    /**
     * 获取byte[]类型响应信息
     */
    byte[] getResult();

    default String getCookie(String name) {
        List<Header> cookieList = getHeaderManager().getHeader(HttpHeaders.RESPONSE_COOKIE);
        for (Header header : cookieList) {
            if (header.containsKey(name)) {
                return header.getInternalValue(name);
            }
        }
        return null;
    }

    default List<Header> getCookies() {
        return getHeaderManager().getHeader(HttpHeaders.RESPONSE_COOKIE);
    }

    /**
     * 获取String类型的响应信息
     */
    default String getStringResult() {
        Charset charset = getContentType().getCharset();
        return new String(getResult(), charset);
    }

    /**
     * 获取InputStream类型的响应信息
     */
    default InputStream getInputStream() {
        return new ByteArrayInputStream(getResult());
    }

    /**
     * 获取InputStreamSource类型的响应信息
     */
    default InputStreamSource getInputStreamSource() {
        return this::getInputStream;
    }

    /**
     * 获取MultipartFile类型的响应信息
     */
    default MultipartFile getMultipartFile() {
        Header header = getHeaderManager().getFirstHeader(HttpHeaders.CONTENT_DISPOSITION);
        if (header == null) {
            throw new ResponseProcessException("There is no response header named 'Content-Disposition', so the response result cannot be converted to 'com.luckyframework.io.MultipartFile'.");
        }
        if (!header.containsKey("filename")) {
            throw new ResponseProcessException("The 'filename' attribute is not provided in the response header 'Content-Disposition', so the response result cannot be converted to 'com.luckyframework.io.MultipartFile'.");
        }
        String filename = header.getInternalValue("filename");
        filename = filename.startsWith("\"") ? filename.substring(1) : filename;
        filename = filename.endsWith("\"") ? filename.substring(0, filename.length() - 1) : filename;
        return new MultipartFile(getInputStream(), filename);
    }

    /**
     * 自动将响应的数据转化为实体类
     * 只支持Content-Type=APPLICATION_JSON、APPLICATION_XML和TEXT_XML
     * 如果无法转化或者转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param type 实体的泛型
     * @param <T>  类型
     * @return
     */
    @SuppressWarnings("unchecked")
    default <T> T getEntity(Type type) {
        // 文件、流类型的结果处理
        if (type == MultipartFile.class) {
            return (T) getMultipartFile();
        }
        if (InputStream.class == type || ByteArrayInputStream.class == type) {
            return (T) getInputStream();
        }
        if (type == InputStreamSource.class) {
            return (T) getInputStreamSource();
        }
        if (type == byte[].class) {
            return (T) getResult();
        }

        String strResult = getStringResult();
        if (type == String.class) {
            return (T) strResult;
        }

        ContentType contentType = getContentType();
        try {
            if (contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())) {
                return (T) jsonScheme.deserialization(strResult, type);
            }
            if (contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_XML.getMimeType())
                    || contentType.equals(ContentType.TEXT_XML)) {
                return (T) xmlScheme.deserialization(strResult, type);
            }
            throw new SerializationException("This method only supports the conversion of response bodies of type 'JSON' and 'XML'.");
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 自动将响应的数据转化为实体类
     * 只支持Content-Type=APPLICATION_JSON、APPLICATION_XML和TEXT_XML
     * 如果无法转化或者转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param typeToken 实体的泛型的Token
     * @param <T>       类型
     * @return
     */
    default <T> T getEntity(SerializationTypeToken<T> typeToken) {
        return getEntity(typeToken.getType());
    }

    /**
     * 自动将响应的数据转化为实体类
     * 只支持Content-Type=APPLICATION_JSON、APPLICATION_XML和TEXT_XML
     * 如果无法转化或者转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param entityClass 实体的Class
     * @param <T>         类型
     * @return
     */
    default <T> T getEntity(Class<T> entityClass) {
        return getEntity((Type) entityClass);
    }

    /**
     * 将的到的响应转化为JSON字符后再将字符串转化为实体，
     * 转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param type 实体的泛型
     * @param <T>  类型
     * @return
     */
    @SuppressWarnings("unchecked")
    default <T> T jsonStrToEntity(Type type) {
        try {
            return (T) jsonScheme.deserialization(getStringResult(), type);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 将的到的响应转化为JSON字符后再将字符串转化为实体，
     * 转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param typeToken 实体的泛型的Token
     * @param <T>       类型
     * @return
     */
    default <T> T jsonStrToEntity(SerializationTypeToken<T> typeToken) {
        return jsonStrToEntity(typeToken.getType());
    }

    /**
     * 将的到的响应转化为JSON字符后再将字符串转化为实体，
     * 转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param entityClass 实体的Class
     * @param <T>         类型
     * @return
     */
    default <T> T jsonStrToEntity(Class<T> entityClass) {
        return jsonStrToEntity((Type) entityClass);
    }

    /**
     * 将的到的响应转化为XML字符后再将字符串转化为实体，
     * 转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param type 实体的泛型
     * @param <T>  类型
     * @return
     */
    @SuppressWarnings("unchecked")
    default <T> T xmlStrToEntity(Type type) {
        try {
            return (T) xmlScheme.deserialization(getStringResult(), type);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 将的到的响应转化为XML字符后再将字符串转化为实体，
     * 转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param typeToken 实体的泛型Token
     * @param <T>       类型
     * @return
     */
    default <T> T xmlStrToEntity(SerializationTypeToken<T> typeToken) {
        return xmlStrToEntity(typeToken.getType());
    }

    /**
     * 将的到的响应转化为XML字符后再将字符串转化为实体，
     * 转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param entityClass 实体的Class
     * @param <T>         类型
     * @return
     */
    default <T> T xmlStrToEntity(Class<T> entityClass) {
        return xmlStrToEntity((Type) entityClass);
    }


    default <T> T toEntity(ResponseConvert responseConvert, Type type) throws Exception {
        return responseConvert.convert(this, type);
    }

}
