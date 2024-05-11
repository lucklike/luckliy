package com.luckyframework.httpclient.core;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.core.impl.DefaultResponse;
import com.luckyframework.io.MultipartFile;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.luckyframework.httpclient.core.SerializationConstant.*;

/**
 * 响应接口
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/28 8:21 下午
 */
public interface Response {

    /**
     * 结果自动转换器
     */
    List<AutoConvert> autoConvertList = new ArrayList<>();

    /**
     * 添加一个结果自动转换器
     *
     * @param autoConvert 结果自动转换器
     */
    static void addAutoConvert(AutoConvert autoConvert) {
        autoConvertList.add(autoConvert);
    }

    /**
     * 添加一个结果自动转换器
     *
     * @param index 索引位置
     * @param autoConvert 结果自动转换器
     */
    static void addAutoConvert(int index, AutoConvert autoConvert) {
        autoConvertList.add(index, autoConvert);
    }


    /**
     * 获取当前请求信息
     *
     * @return 当前请求信息
     */
    Request getRequest();

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    int getStatus();

    /**
     * 获取响应头管理器
     *
     * @return 响应头管理器
     */
    HttpHeaderManager getHeaderManager();

    /**
     * 获取byte[]类型响应信息
     *
     * @return byte[]类型响应信息
     */
    byte[] getResult();

    /**
     * 获取响应元数据
     *
     * @return 响应元数据
     */
    ResponseMetaData getResponseMetaData();


    //------------------------------------------------------------------------------
    //                            default methods
    //------------------------------------------------------------------------------

    /**
     * 获取响应体长度（单位：字节）
     *
     * @return 响应体长度
     */
    default long getContentLength() {
        try {
            long contentLength = getResponseMetaData().getContentLength();
            if (contentLength == -1) {
                return getResult().length;
            }
            return contentLength;
        } catch (Exception e) {
            return getResult().length;
        }
    }

    /**
     * 获取响应的Content-Type
     *
     * @return 响应Content-Type
     */
    default ContentType getContentType() {
        return getResponseMetaData().getContentType();
    }

    default String getCookie(String name) {
        return getResponseMetaData().getCookie(name);
    }

    default List<Header> getCookies() {
        return getResponseMetaData().getCookies();
    }

    default List<ClientCookie> getResponseCookies() {
        return getCookies().stream().map(h -> new ClientCookie(h, this.getRequest())).collect(Collectors.toList());
    }

    default Map<String, Object> getSimpleCookies() {
        List<ClientCookie> responseCookies = getResponseCookies();
        Map<String, Object> cookieMap = new HashMap<>(responseCookies.size());
        responseCookies.forEach(cookie -> cookieMap.put(cookie.getName(), cookie.getValue()));
        return cookieMap;
    }

    default Map<String, Object> getSimpleHeaders() {
        return getResponseMetaData().getSimpleHeaders();
    }


    /**
     * 获取String类型的响应信息
     */
    default String getStringResult() {
        return getStringResult(getContentType().getCharset());
    }

    /**
     * 获取String类型的响应信息，并指定编码
     *
     * @param charset 编码方式
     */
    default String getStringResult(Charset charset) {
        return new String(getResult(), charset == null ? StandardCharsets.UTF_8 : charset);
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
        return new MultipartFile(getInputStream(), ResourceNameParser.getResourceName(getResponseMetaData()));
    }

    /**
     * 获取响应内容信息
     *
     * @return 响应内容信息
     */
    @Nullable
    default ContentInfo getResultContentInfo() {
        return new ContentInfoUtil().findMatch(getResult());
    }

    /**
     * 自动将响应的数据转化为实体类
     * 只支持Content-Type=APPLICATION_JSON、APPLICATION_XML、TEXT_XML、APPLICATION_JAVA_SERIALIZED_OBJECT
     * 如果无法转化或者转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param type 实体的泛型
     * @param <T>  类型
     * @return type类型的实体对象
     */
    @SuppressWarnings("unchecked")
    default <T> T getEntity(Type type) {

        // 先尝试使用自动转换器进行转换
        for (AutoConvert autoConvert : autoConvertList) {
            if (autoConvert.can(this)) {
                return autoConvert.convert(this, type);
            }
        }

        // 固定类型转换
        if (Response.class == type || DefaultResponse.class == type) {
            return (T) this;
        }
        if (VoidResponse.class == type) {
            return (T) new VoidResponse(this.getResponseMetaData());
        }
        if (ResponseMetaData.class == type) {
            return (T) this.getResponseMetaData();
        }
        // 文件、流类型的结果处理
        if (MultipartFile.class == type) {
            return (T) getMultipartFile();
        }
        if (InputStream.class == type || ByteArrayInputStream.class == type) {
            return (T) getInputStream();
        }
        if (InputStreamSource.class == type) {
            return (T) getInputStreamSource();
        }
        if (byte[].class == type) {
            return (T) getResult();
        }

        if (String.class == type) {
            return (T) getStringResult();
        }

        // Json、Xml、Java类型转换
        try {
            if (isJsonType()) {
                return jsonStrToEntity(type);
            }
            if (isXmlType()) {
                return xmlStrToEntity(type);
            }
            if (isJavaType()) {
                return (T) javaObject();
            }
            throw new SerializationException("The response result the auto-conversion is abnormal: No converter found that can handle 'Content-Type[" + getContentType() + "]'.");
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    /**
     * 自动将响应的数据转化为实体类
     * 只支持Content-Type=APPLICATION_JSON、APPLICATION_XML、TEXT_XML、APPLICATION_JAVA_SERIALIZED_OBJECT
     * 如果无法转化或者转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param typeToken 实体的泛型的Token
     * @param <T>       类型
     * @return typeToken类型对应的实体对象
     */
    default <T> T getEntity(SerializationTypeToken<T> typeToken) {
        return getEntity(typeToken.getType());
    }

    /**
     * 自动将响应的数据转化为实体类
     * 只支持Content-Type=APPLICATION_JSON、APPLICATION_XML、TEXT_XML、APPLICATION_JAVA_SERIALIZED_OBJECT
     * 如果无法转化或者转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param entityClass 实体的Class
     * @param <T>         类型
     * @return entityClass类型对应的实体对象
     */
    default <T> T getEntity(Class<T> entityClass) {
        return getEntity((Type) entityClass);
    }


    /**
     * 获取一个Map类型的结果
     *
     * @return Map类型的结果
     */
    default Map<String, Object> getMapResult() {
        return getEntity(new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    /**
     * 获取一个List&lt;Map&lt;String, Object>>类型的结果
     *
     * @return List&lt;Map&lt;String, Object>>类型的结果
     */
    default List<Map<String, Object>> getMapListResult() {
        return getEntity(new SerializationTypeToken<List<Map<String, Object>>>() {
        });
    }

    /**
     * 获取一个ConfigurationMap类型的结果
     *
     * @return ConfigurationMap类型的结果
     */
    default ConfigurationMap getConfigMapResult() {
        ConfigurationMap configurationMap = new ConfigurationMap();
        configurationMap.addProperties(getMapResult());
        return configurationMap;
    }

    /**
     * 获取一个List&lt;ConfigurationMap>类型的结果
     *
     * @return List&lt;ConfigurationMap>类型的结果
     */
    default List<ConfigurationMap> getConfigMapListResult() {
        return getMapListResult().stream().map(map -> {
            ConfigurationMap configurationMap = new ConfigurationMap();
            configurationMap.addProperties(map);
            return configurationMap;
        }).collect(Collectors.toList());
    }

    /**
     * 将的到的响应转化为JSON字符后再将字符串转化为实体，
     * 转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param type 实体的泛型
     * @param <T>  类型
     * @return type类型对应的实体对象
     */
    @SuppressWarnings("unchecked")
    default <T> T jsonStrToEntity(Type type) {
        try {
            return (T) JSON_SCHEME.deserialization(getStringResult(), type);
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
     * @return typeToken类型对应的实体对象
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
     * @return entityClass类型对应的实体对象
     */
    default <T> T jsonStrToEntity(Class<T> entityClass) {
        return jsonStrToEntity((Type) entityClass);
    }

    /**
     * 将的到的响应转化为JSON字符后再将字符串转化为Map
     *
     * @return Map类型结果
     */
    default Map<String, Object> jsonStrToMap() {
        return jsonStrToEntity(new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    /**
     * 将的到的响应转化为JSON字符后再将字符串转化为List&lt;Map&lt;String, Object>>
     *
     * @return ist&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> jsonStrToMapList() {
        return jsonStrToEntity(new SerializationTypeToken<List<Map<String, Object>>>() {
        });
    }

    /**
     * 将的到的响应转化为JSON字符后再将字符串转化为ConfigurationMap
     *
     * @return ConfigurationMap类型结果
     */
    default ConfigurationMap jsonStrToConfigMap() {
        ConfigurationMap configMap = new ConfigurationMap();
        configMap.addProperties(jsonStrToMap());
        return configMap;
    }

    /**
     * 将的到的响应转化为JSON字符后再将字符串转化为List&lt;ConfigurationMap>
     *
     * @return List&lt;ConfigurationMap>类型结果
     */
    default List<ConfigurationMap> jsonStrToConfigMapList() {
        return jsonStrToMapList().stream().map(map -> {
            ConfigurationMap configMap = new ConfigurationMap();
            configMap.addProperties(map);
            return configMap;
        }).collect(Collectors.toList());
    }

    /**
     * 将的到的响应转化为XML字符后再将字符串转化为实体，
     * 转化失败会时抛出一个{@link SerializationException}异常
     *
     * @param type 实体的泛型
     * @param <T>  类型
     * @return type类型对应的实体对象
     */
    @SuppressWarnings("unchecked")
    default <T> T xmlStrToEntity(Type type) {
        try {
            return (T) XML_SCHEME.deserialization(getStringResult(), type);
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
     * @return typeToken类型对应的实体对象
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
     * @return entityClass类型对应的实体对象
     */
    default <T> T xmlStrToEntity(Class<T> entityClass) {
        return xmlStrToEntity((Type) entityClass);
    }

    /**
     * 将的到的响应转化为XML字符后再将字符串转化为Map
     *
     * @return Map类型结果
     */
    default Map<String, Object> xmlStrToMap() {
        return xmlStrToEntity(new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    /**
     * 将的到的响应转化为XML字符后再将字符串转化为List&lt;Map&lt;String, Object>>
     *
     * @return ist&lt;Map&lt;String, Object>>类型结果
     */
    default List<Map<String, Object>> xmlStrToMapList() {
        return xmlStrToEntity(new SerializationTypeToken<List<Map<String, Object>>>() {
        });
    }

    /**
     * 将的到的响应转化为XML字符后再将字符串转化为ConfigurationMap
     *
     * @return ConfigurationMap类型结果
     */
    default ConfigurationMap xmlStrToConfigMap() {
        ConfigurationMap configMap = new ConfigurationMap();
        configMap.addProperties(xmlStrToMap());
        return configMap;
    }

    /**
     * 将的到的响应转化为XML字符后再将字符串转化为List&lt;ConfigurationMap>
     *
     * @return List&lt;ConfigurationMap>类型结果
     */
    default List<ConfigurationMap> xmlStrToConfigMapList() {
        return xmlStrToMapList().stream().map(map -> {
            ConfigurationMap configMap = new ConfigurationMap();
            configMap.addProperties(map);
            return configMap;
        }).collect(Collectors.toList());
    }

    /**
     * 将使用Java序列化后的信息反序列化为Java对象
     *
     * @return 反序列化后的Java对象
     */
    default Object javaObject() {
        try {
            return JDK_SCHEME.fromByte(getResult());
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    default <T> T javaEntity() {
        return (T) javaObject();
    }

    /**
     * 当前请求是否是返回体为application/json类型的
     */
    default boolean isJsonType() {
        return getContentType().getMimeType().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType());
    }

    /**
     * 当前请求是否是返回体为application/xml或text/xml者类型的
     */
    default boolean isXmlType() {
        ContentType contentType = getContentType();
        return contentType.getMimeType().equalsIgnoreCase(ContentType.APPLICATION_XML.getMimeType())
                || contentType.equals(ContentType.TEXT_XML);
    }

    /**
     * 当前请求是否是返回体为application/x-java-serialized-object类型的
     */
    default boolean isJavaType() {
        return getContentType().getMimeType().equalsIgnoreCase(ContentType.APPLICATION_JAVA_SERIALIZED_OBJECT.getMimeType());
    }

    /**
     * 结果自动转换器
     */
    interface AutoConvert {
        boolean can(Response resp);

        <T> T convert(Response resp, Type type);
    }

}
