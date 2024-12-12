package com.luckyframework.httpclient.core.meta;

import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationScheme;
import org.springframework.lang.NonNull;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于Map的Body对象工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/12/13 02:41
 */
public class MapBodyObjectFactory implements BodyObjectFactory {

    private final SerializationScheme serializationScheme;
    private final Map<String, Object> map;
    private final String mimeType;
    private final Charset charset;

    private MapBodyObjectFactory(SerializationScheme serializationScheme, String mimeType, Charset charset) {
        this.map = new LinkedHashMap<>();
        this.serializationScheme = serializationScheme;
        this.mimeType = mimeType;
        this.charset = charset;
    }

    public static MapBodyObjectFactory create(SerializationScheme serializationScheme, String mimeType, Charset charset) {
        return new MapBodyObjectFactory(serializationScheme, mimeType, charset);
    }

    public static MapBodyObjectFactory create(SerializationScheme serializationScheme, ContentType contentType) {
        return create(serializationScheme, contentType.getMimeType(), contentType.getCharset());
    }

    public void addProperty(String key, Object value) {
        map.put(key, value);
    }

    @NonNull
    @Override
    public BodyObject create() {
        try {
            return BodyObject.builder(mimeType, charset, serializationScheme.serialization(map));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
