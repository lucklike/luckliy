package com.luckyframework.httpclient.core.meta;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationScheme;
import org.springframework.lang.NonNull;

import java.nio.charset.Charset;

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

    private ConfigurationMapBodyObjectFactory(String dataKey, SerializationScheme serializationScheme, String mimeType, Charset charset) {
        this.dataKey = dataKey;
        this.configMap = new ConfigurationMap();
        this.serializationScheme = serializationScheme;
        this.mimeType = mimeType;
        this.charset = charset;
    }

    public static ConfigurationMapBodyObjectFactory of(String dataKey, SerializationScheme serializationScheme, String mimeType, Charset charset) {
        return new ConfigurationMapBodyObjectFactory(dataKey, serializationScheme, mimeType, charset);
    }

    public static ConfigurationMapBodyObjectFactory of(SerializationScheme serializationScheme, String mimeType, Charset charset) {
        return of(null, serializationScheme, mimeType, charset);
    }

    public static ConfigurationMapBodyObjectFactory of(String dataKey, SerializationScheme serializationScheme, ContentType contentType) {
        return of(dataKey, serializationScheme, contentType.getMimeType(), contentType.getCharset());
    }

    public static ConfigurationMapBodyObjectFactory of(SerializationScheme serializationScheme, ContentType contentType) {
        return of(null, serializationScheme, contentType);
    }

    public static ConfigurationMapBodyObjectFactory json(String dataKey) {
        return of(dataKey, JSON_SCHEME, APPLICATION_JSON);
    }

    public static ConfigurationMapBodyObjectFactory json() {
        return json(null);
    }


    public void addProperty(String key, Object value) {
        configMap.addProperty(key, value);
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
