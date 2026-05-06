package com.luckyframework.httpclient.core.util;

import org.springframework.core.ResolvableType;

import java.util.Map;

/**
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/4/16 01:16
 */
public class MapPropertyInfo implements PropertyInfo {

    /**
     * Map
     */
    private final Map<String, Object> map;

    /**
     * Map Key
     */
    private final String key;

    /**
     * Map Value
     */
    private final Object value;

    /**
     * Value Type
     */
    private final ResolvableType type;

    public MapPropertyInfo(Map<String, Object> map,String key, Object value) {
        this(map, key, value, ResolvableType.forInstance(value));
    }

    public MapPropertyInfo(Map<String, Object> map,String key, Object value, ResolvableType type) {
        this.map = map;
        this.key = key;
        this.value = value;
        this.type = type;
    }

    @Override
    public Object newObject() {
        return null;
    }

    @Override
    public String getName() {
        return key;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        map.put(key, value);
    }

    @Override
    public ResolvableType getResolvableType() {
        return type;
    }

}
