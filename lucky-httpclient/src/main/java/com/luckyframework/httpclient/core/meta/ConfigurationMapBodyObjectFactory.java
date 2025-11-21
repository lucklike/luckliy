package com.luckyframework.httpclient.core.meta;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationScheme;
import org.springframework.lang.NonNull;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.luckyframework.httpclient.core.meta.ContentType.APPLICATION_JSON;
import static com.luckyframework.httpclient.core.serialization.SerializationConstant.JSON_SCHEME;

/**
 * 基于{@link ConfigurationMap}实现的Body对象工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/13 02:41
 */
public class ConfigurationMapBodyObjectFactory implements BodyObjectFactory {

    private final String dataKey;
    private final SerializationScheme serializationScheme;
    private final ConfigurationMap configMap;
    private final String mimeType;
    private final Charset charset;

    private ConfigurationMapBodyObjectFactory(String dataKey, SerializationScheme serializationScheme, Map<String, Object> configMap, String mimeType, Charset charset) {
        this.dataKey = dataKey;
        this.configMap = new ConfigurationMap(configMap == null ? new LinkedHashMap<>() : configMap);
        this.serializationScheme = serializationScheme;
        this.mimeType = mimeType;
        this.charset = charset;
    }

    public static ConfigurationMapBodyObjectFactory of(String dataKey, SerializationScheme serializationScheme, Map<String, Object> configMap, String mimeType, Charset charset) {
        return new ConfigurationMapBodyObjectFactory(dataKey, serializationScheme, configMap, mimeType, charset);
    }

    public static ConfigurationMapBodyObjectFactory of(String dataKey, SerializationScheme serializationScheme, String mimeType, Charset charset) {
        return of(dataKey, serializationScheme, null, mimeType, charset);
    }

    public static ConfigurationMapBodyObjectFactory of(SerializationScheme serializationScheme, Map<String, Object> configMap, String mimeType, Charset charset) {
        return of(null, serializationScheme, configMap, mimeType, charset);
    }

    public static ConfigurationMapBodyObjectFactory of(SerializationScheme serializationScheme, String mimeType, Charset charset) {
        return of(serializationScheme, null, mimeType, charset);
    }

    public static ConfigurationMapBodyObjectFactory of(String dataKey, SerializationScheme serializationScheme, Map<String, Object> configMap, ContentType contentType) {
        return of(dataKey, serializationScheme, configMap, contentType.getMimeType().toString(), contentType.getCharset());
    }


    public static ConfigurationMapBodyObjectFactory of(String dataKey, SerializationScheme serializationScheme, ContentType contentType) {
        return of(dataKey, serializationScheme, null, contentType);
    }

    public static ConfigurationMapBodyObjectFactory of(SerializationScheme serializationScheme, Map<String, Object> configMap, ContentType contentType) {
        return of(null, serializationScheme, configMap, contentType);
    }

    public static ConfigurationMapBodyObjectFactory of(SerializationScheme serializationScheme, ContentType contentType) {
        return of(serializationScheme, null, contentType);
    }

    public static ConfigurationMapBodyObjectFactory json(String dataKey, Map<String, Object> configMap) {
        return of(dataKey, JSON_SCHEME, configMap, APPLICATION_JSON);
    }

    public static ConfigurationMapBodyObjectFactory json(String dataKey) {
        return json(dataKey, null);
    }

    public static ConfigurationMapBodyObjectFactory json(Map<String, Object> configMap) {
        return json(null, configMap);
    }

    public static ConfigurationMapBodyObjectFactory json() {
        return json(null, null);
    }

    public void addProperty(String key, Object value) {
        configMap.addProperty(key, value);
    }

    /**
     * 获取请求体参数工厂
     *
     * @param request   请求实例
     * @param configMap 资源数据
     * @param dataKey   目标数据所在KEY
     * @return 请求体参数工厂
     */
    public static synchronized ConfigurationMapBodyObjectFactory forRequest(Request request, Map<String, Object> configMap, String dataKey) {
        BodyObjectFactory bodyFactory = request.getBodyFactory();
        if (!(bodyFactory instanceof ConfigurationMapBodyObjectFactory)) {
            bodyFactory = ConfigurationMapBodyObjectFactory.json(dataKey, configMap);
            request.setContentType(APPLICATION_JSON);
            request.setBodyFactory(bodyFactory);
        }
        return (ConfigurationMapBodyObjectFactory) bodyFactory;
    }

    /**
     * 获取请求体参数工厂
     *
     * @param request 请求实例
     * @param dataKey 目标数据所在KEY
     * @return 请求体参数工厂
     */
    public static synchronized ConfigurationMapBodyObjectFactory forRequest(Request request, String dataKey) {
        return forRequest(request, null, dataKey);
    }

    public void put(String key, Object value) {
        configMap.put(key, value);
    }

    @NonNull
    @Override
    public BodyObject create() {
        try {
            Object data = StringUtils.hasText(dataKey) ? configMap.getProperty(dataKey) : configMap;
            return BodyObject.builder(mimeType, charset, serializationScheme.serialization(data));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
