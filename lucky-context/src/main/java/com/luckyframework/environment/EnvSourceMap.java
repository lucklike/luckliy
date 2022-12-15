package com.luckyframework.environment;

import com.luckyframework.common.ConfigurationMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/15 09:23
 */
@SuppressWarnings("all")
final class EnvSourceMap implements EnvData,Map<String, Object>{

    private static final Map<String, ConfigurationMap> sourceMapChace = new ConcurrentHashMap<>();
    private final ConfigurationMap sourceMap;


    public EnvSourceMap(ObjectPropertySourcesPropertyResolver resolver, String mapPropName) {
        ConfigurationMap configurationMap = sourceMapChace.get(mapPropName);
        if(configurationMap == null){
            configurationMap = new ConfigurationMap();
            List<Object> envValues = getEnvironmentValues(resolver, mapPropName);

            // 如果环境中之存在一组匹配值，则直接使用该值来构造
            if(envValues.size() == 1){
                Object sourceValue = envValues.get(0);
                if(sourceValue instanceof ConfigurationMap){
                    configurationMap = (ConfigurationMap) sourceValue;
                } else if(sourceValue instanceof Map){
                    configurationMap.addConfigProperties((Map)sourceValue);
                }
            }
            // 如果环境中存在多组匹配值，则需要将这多组值进行融合
            else{
                configurationMap = crerateSourceMap(resolver, envValues, mapPropName);
            }

            // 存入缓存
            sourceMapChace.put(mapPropName, configurationMap);
        }

        sourceMap = configurationMap;
    }

    private ConfigurationMap crerateSourceMap(ObjectPropertySourcesPropertyResolver resolver, List<Object> envValues, String mapPropName) {
        Set<String> mapKeySet = getAllKeys(envValues);
        ConfigurationMap configurationMap = new ConfigurationMap();
        for (String mapKey : mapKeySet) {

            String propKey = getEntryPropName(mapPropName, mapKey);
            Object propValue = resolver.getPropertyForObject(propKey);

            // EnvData类型和null不做处理
            if((propValue instanceof EnvData) || propValue == null){
                configurationMap.put(mapKey, propValue);
                break;
            }

            // 集合或者数组统一转换成EnvSourceList类型
            if(((propValue instanceof Collection) || propValue.getClass().isArray())){
                configurationMap.put(mapKey, new EnvSourceList(resolver, propKey));
            }

            // Map类型统一转化为EnvSourceMap类型
            else if(propValue instanceof Map){
                configurationMap.put(mapKey, new EnvSourceMap(resolver, propKey));
            }

            // 其他类型均不做处理
            else{
                configurationMap.put(mapKey, propValue);
            }
        }
        return configurationMap;
    }

    private Set<String> getAllKeys(List<Object> envValues){
        Set<String> mapKeySet = new LinkedHashSet<>();
        for (Object value : envValues) {
            if(value instanceof Map){
                mapKeySet.addAll(((Map) value).keySet());
            }
        }
        return mapKeySet;
    }

    private String getEntryPropName(String propName, String mapKey){
        return propName + "." + mapKey;
    }

    @Override
    public int size() {
        return sourceMap.size();
    }

    @Override
    public boolean isEmpty() {
        return sourceMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return sourceMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return sourceMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        if(key instanceof String){
            String strKey = (String)key;
            Object value = sourceMap.get(strKey);
            return value == null ? sourceMap.getConfigProperty(strKey) : value;
        }
        return sourceMap.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return sourceMap.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return sourceMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        sourceMap.putAll(m);
    }

    @Override
    public void clear() {
        sourceMap.clear();
    }

    public Set<String> keySet(){
        return sourceMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return sourceMap.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return sourceMap.entrySet();
    }


}
