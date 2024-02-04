package com.luckyframework.common;

import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.lang.NonNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置Map
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/29 下午5:18
 */
public class ConfigurationMap implements Map<String, Object>, SupportsStringManipulationMap {

    private final Map<String, Object> configurationMap = new ConcurrentHashMap<>();
    public static final NullEntry NULL_ENTRY = new NullEntry();

    public ConfigurationMap() {
    }

    public ConfigurationMap(Map<String, Object> map) {
        putAll(map);
    }

    public ConfigurationMap(Object entity) {
        putEntity(entity);
    }

    public static ConfigurationMap create(Properties properties) {
        ConfigurationMap configMap = new ConfigurationMap();
        Set<String> propertyNames = properties.stringPropertyNames();
        for (String propertyName : propertyNames) {
            configMap.addProperty(propertyName, properties.get(propertyName));
        }
        return configMap;
    }


    //------------------------------------------------------------------
    //                     MapUtils Code Methods
    //------------------------------------------------------------------

    public Map<String, Object> getDataMap() {
        return this.configurationMap;
    }

    @Override
    public Object getProperty(String configKey) {
        Object value = MapUtils.get(configurationMap, configKey);
        return value == NULL_ENTRY ? null : value;
    }

    @Override
    public ConfigurationMap getMap(String configKey) {
        return getEntry(configKey, ConfigurationMap.class);
    }

    @Override
    public List<ConfigurationMap> getMapList(String configKey) {
        return getEntry(configKey, new SerializationTypeToken<List<ConfigurationMap>>() {
        });
    }


    @Override
    public void addProperty(String configKey, Object confValue) {
        MapUtils.put(configurationMap, configKey, getNotNullEntry(confValue));
    }

    @Override
    public void addProperties(Map<?, ?> properties) {
        for (Entry<?, ?> entry : properties.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            // 元素是Map的情况
            if (value instanceof Map) {
                ConfigurationMap configMap = new ConfigurationMap();
                configMap.addProperties((Map<?, ?>) value);
                addProperty(key, configMap);
            }
            // 元素为可迭代对象，且元素类型为Map时，统一转化为ArrayList
            else if (ContainerUtils.isIterable(value)) {
                List<Object> list = new ArrayList<>(ContainerUtils.getIteratorLength(value));
                Iterator<Object> iterator = ContainerUtils.getIterator(value);
                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    if (next instanceof Map) {
                        ConfigurationMap configMap = new ConfigurationMap();
                        configMap.addProperties((Map<?, ?>) next);
                        list.add(configMap);
                    } else {
                        list.add(next);
                    }
                }
                addProperty(key, list);
            } else {
                addProperty(key, value);
            }
        }
    }

    public Properties toProperties(boolean ignoreNullValue) {
        Properties properties = new Properties();
        objectTileProperties(properties, "", this, ignoreNullValue);
        return properties;
    }

    public Properties toProperties() {
        return toProperties(false);
    }

    public void objectTileProperties(@NonNull Properties properties, String prefixKey, Object value, boolean ignoreNullValue) {
        if (value == null || value == NULL_ENTRY) {
            if (!ignoreNullValue) {
                throw new IllegalArgumentException("ConfigurationMap the key '" + removeEndOfPoint(prefixKey) + "' corresponds value is null.");
//                properties.setProperty(removeEndOfPoint(prefixKey), "");
            }
        } else if (value instanceof Map) {
            mapTileProperties(properties, prefixKey, (Map<?, ?>) value, ignoreNullValue);
        } else if (value instanceof Collection) {
            collectionTileProperties(properties, prefixKey, (Collection<?>) value, ignoreNullValue);
        } else if (value.getClass().isArray()) {
            arrayTileProperties(properties, prefixKey, value, ignoreNullValue);
        } else {
            properties.setProperty(removeEndOfPoint(prefixKey), value.toString());
        }
    }

    public void mapTileProperties(@NonNull Properties properties, String prefixKey, Map<?, ?> map, boolean ignoreNullValue) {
        for (Entry<?, ?> entry : map.entrySet()) {
            objectTileProperties(properties, prefixKey + entry.getKey() + ".", entry.getValue(), ignoreNullValue);
        }
    }

    public void mapTileProperties(@NonNull Properties properties, String prefixKey, Map<?, ?> map) {
        mapTileProperties(properties, prefixKey, map, false);
    }

    public void collectionTileProperties(@NonNull Properties properties, String prefixKey, Collection<?> collection, boolean ignoreNullValue) {
        int index = 0;
        for (Object collectionElement : collection) {
            objectTileProperties(properties, removeEndOfPoint(prefixKey) + "[" + (index++) + "].", collectionElement, ignoreNullValue);
        }
    }

    public void collectionTileProperties(@NonNull Properties properties, String prefixKey, Collection<?> collection) {
        collectionTileProperties(properties, prefixKey, collection, false);
    }


    public void arrayTileProperties(@NonNull Properties properties, String prefixKey, Object array, boolean ignoreNullValue) {
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            objectTileProperties(properties, removeEndOfPoint(prefixKey) + "[" + i + "].", Array.get(array, i), ignoreNullValue);
        }
    }

    public void arrayTileProperties(@NonNull Properties properties, String prefixKey, Object array) {
        arrayTileProperties(properties, prefixKey, array, false);
    }

    private String removeEndOfPoint(String prefixKey) {
        return prefixKey.endsWith(".") ? prefixKey.substring(0, prefixKey.length() - 1) : prefixKey;
    }

    @Override
    public void addAsItExists(String configKey, Object confValue) {
        if (containsConfigKey(configKey)) {
            addProperty(configKey, getNotNullEntry(confValue));
        }
    }

    @Override
    public void addAsItExists(Map<String, Object> properties) {
        for (Entry<String, Object> entry : properties.entrySet()) {
            addAsItExists(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean containsConfigKey(String configKey) {
        return MapUtils.containsKey(configurationMap, configKey);
    }

    @Override
    public Object removeConfigProperty(String configKey) {
        return MapUtils.remove(configurationMap, configKey);
    }

    @Override
    public void mergeConfig(Map<String, Object> newConfigMap) {
        MapUtils.weakFusionMap(configurationMap, newConfigMap);
    }

    @Override
    public Object putIn(String sourceKey, String newKey, Object newValue) {
        return MapUtils.put(configurationMap, sourceKey, newKey, getNotNullEntry(newValue));
    }

    @Override
    public void putEntity(Object entity) {
        if (entity instanceof Map) {
            addProperties(((Map<?, ?>) entity));
        }
        Map<String, Object> map = MapUtils.entityToMap(entity);
        addProperties(map);
    }


    //------------------------------------------------------------------
    //                          Map Methods
    //------------------------------------------------------------------


    @Override
    public int size() {
        return configurationMap.size();
    }

    @Override
    public boolean isEmpty() {
        return configurationMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return configurationMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return configurationMap.containsValue(getNotNullEntry(value));
    }

    @Override
    public Object get(Object key) {
        return configurationMap.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return configurationMap.put(key, getNotNullEntry(value));
    }

    @Override
    public Object remove(Object key) {
        return configurationMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        configurationMap.putAll(m);
    }

    @Override
    public void clear() {
        configurationMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return configurationMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return configurationMap.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return configurationMap.entrySet();
    }

    @Override
    public String toString() {
        return configurationMap.toString();
    }

    public static Object getNotNullEntry(Object mayBeEmptyValue) {
        return mayBeEmptyValue == null ? NULL_ENTRY : mayBeEmptyValue;
    }

    public static Object getMayBeNullValue(Object mayBeNullEntry) {
        return mayBeNullEntry == NULL_ENTRY ? null : mayBeNullEntry;
    }

    public final static class NullEntry {

    }
}
