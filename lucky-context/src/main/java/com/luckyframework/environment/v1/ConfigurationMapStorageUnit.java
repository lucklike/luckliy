package com.luckyframework.environment.v1;

import com.luckyframework.common.*;
import com.luckyframework.exception.GetConfigurationInfoException;
import org.springframework.util.Assert;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 以ConfigurationMap为基础的数据单元
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/29 下午9:22
 */
public class ConfigurationMapStorageUnit implements StorageUnit{

    private final ConfigurationMap configMap = new ConfigurationMap();
    private ConfigurationMap realMap;

    public ConfigurationMapStorageUnit(Map<String,?> data){
        if(data != null){
            configMap.putAll(data);
        }
    }

    public ConfigurationMapStorageUnit(Object entity){
        configMap.putEntity(entity);
    }

    @Override
    public Object getRealValue(String key) {
        Map<String, Object> realMap = getRealMap();
        Object value = realMap.get(key);
        if(value == null){
            value = ((ConfigurationMap)getRealMap()).getConfigProperty(key);
        }
        return value;
    }

    @Override
    public Object changeToReal(Object value) {
        if(value instanceof Collection){
            return getRealCollection((Collection<?>) value);
        }
        //是数组
        if(value.getClass().isArray()){
            return getRealArray((Object[])value);
        }
        //是Map
        if(value instanceof Map){
            return getRealMap((Map<?,?>)value);
        }
        //是其他
        return parsExpression(value);
    }

    @Override
    public Object parsSingleExpression(String single$Expression) {
        String realKey = single$Expression.substring(2,single$Expression.length()-1).trim();
        String defaultValueExpression = null;
        int x = realKey.indexOf(":");
        if(x != -1){
            defaultValueExpression = realKey.substring(x+1);
            realKey = realKey.substring(0,x);
        }
        if(configMap.containsKey(realKey)){
            return parsExpression(configMap.get(realKey));
        }
        if(configMap.containsConfigKey(realKey)){
            return parsExpression(configMap.getConfigProperty(realKey));
        }
        if(defaultValueExpression != null){
            if(NULL_EXPRESSION.equals(defaultValueExpression.trim())){
                return null;
            }
            return parsExpression(defaultValueExpression);
        }
        throw new GetConfigurationInfoException("解析表达式'"+single$Expression+"'时出现错误，在环境中并没有找到该配置。");
    }

    @Override
    public Object parsExpression(Object $Expression) {
        if($Expression instanceof String){
            String prefix= (String) $Expression;
            TempPair<String[], List<String>> pair = StringUtils.regularCut(prefix, Pattern.compile(Regular.$_$));
            List<String> regularExList = pair.getTwo();
            if(ContainerUtils.isEmptyCollection(regularExList)){
                return prefix;
            }

            if(regularExList.size() == 1 && isExpression(prefix)){
                return parsSingleExpression(prefix);
            }

            List<Object> regularValues = new ArrayList<>(regularExList.size());
            for (String ex : regularExList) {
                Object parserValue = parsSingleExpression(ex);
                Assert.notNull(parserValue,StringUtils.format("An exception occurred while parsing the expression '{}', where the parsed value of the '{}' part is null", $Expression, ex));
                regularValues.add(parsExpression(parserValue));
            }
            return StringUtils.misalignedSplice(pair.getOne(), regularValues.toArray(regularValues.toArray(new Object[0])));
        }
        return $Expression;
    }

    @Override
    public Map<String, Object> getRealMap() {
        if(realMap == null){
            realMap = getRealMap(getOriginalMap());
        }
        return this.realMap;
    }

    @Override
    public Map<String, Object> getOriginalMap() {
        return configMap;
    }

    @Override
    public void setProperties(String key, Object value) {
        configMap.addConfigProperty(key, value);
        if(realMap != null){
            realMap.addConfigProperty(key,changeToReal(value));
        }
    }

    private Object getRealArray(Object[] array) {
        Object[] realArray = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            Object entry = array[i];
            //是集合
            if(entry instanceof Collection){
                realArray[i] = getRealCollection((Collection<?>)entry);
            }
            //是数组
            else if(entry.getClass().isArray()){
                realArray[i] = getRealArray((Object[])entry);
            }
            //是Map
            else if(entry instanceof Map){
                realArray[i] = getRealMap((Map<?,?>)entry);
            }
            //是其他
            else{
                realArray[i] = parsExpression(entry);
            }
        }
        return realArray;
    }

    private Object getRealCollection(Collection<?> collection) {
        List<Object> reaList = new ArrayList<>(collection.size());
        for (Object entry : collection) {
            //是集合
            if(entry instanceof Collection){
                reaList.add(getRealCollection((Collection<?>)entry));
            }
            //是数组
            else if(entry.getClass().isArray()){
                reaList.add(getRealArray((Object[])entry));
            }
            //是Map
            else if(entry instanceof Map){
                reaList.add(getRealMap((Map<?,?>)entry));
            }
            //是其他
            else{
                reaList.add(parsExpression(entry));
            }
        }
        return (collection instanceof List) ? reaList : new HashSet<>(reaList);
    }

    private ConfigurationMap getRealMap(Map<?,?> map){
        ConfigurationMap realMap = new ConfigurationMap();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            //是集合
            if(value instanceof Collection){
                realMap.put(key,getRealCollection((Collection<?>)value));
            }
            //是数组
            else if(value.getClass().isArray()){
                realMap.put(key,getRealArray((Object[])value));
            }
            //是Map
            else if(value instanceof Map){
                realMap.put(key,getRealMap((Map<?,?>)value));
            }
            //是其他
            else{
                realMap.put(key,parsExpression(value));
            }
        }
        return realMap;
    }
}
