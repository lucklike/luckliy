package com.luckyframework.environment;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/16 17:03
 */
final class EnvSourceList implements EnvData, List<Object> {

    private static final Map<String, List<Object>> sourceListCache = new ConcurrentHashMap<>();

    private final List<Object> sourceList;

    public EnvSourceList(ObjectPropertySourcesPropertyResolver resolver, String collectionPropName) {
        List<Object> list = sourceListCache.get(collectionPropName);
        if(list == null){
            list = createSourceList(resolver, collectionPropName);
            sourceListCache.put(collectionPropName, list);
        }
        sourceList = list;
    }

    private List<Object> createSourceList(ObjectPropertySourcesPropertyResolver resolver, String collectionPropName) {

        List<Object> values = getEnvironmentValues(resolver, collectionPropName);
        if(!values.isEmpty()){

            List<ConfigurationMap> configMapList = values.stream().map((obj) -> {
                ConfigurationMap configMap = new ConfigurationMap();
                configMap.addProperty(collectionPropName, obj);
                return configMap;
            }).collect(Collectors.toList());

            int size = configMapList.size();
            if(size == 1){
                return ConversionUtils.conversion(configMapList.get(0).getProperty(collectionPropName), new SerializationTypeToken<List<Object>>() {});
            }
            else{
                ConfigurationMap rootConfigMap = configMapList.get(size - 1);
                for (int i = size - 2; i >= 0 ; i--) {
                    ConfigurationMap thisConfigMap = configMapList.get(i);
                    mergeConfigMap(thisConfigMap, rootConfigMap);
                }
                return ConversionUtils.conversion(rootConfigMap.getProperty(collectionPropName), new SerializationTypeToken<List<Object>>() {});
            }
        }
        return Collections.emptyList();
    }

    private void mergeConfigMap(ConfigurationMap source, ConfigurationMap merge){
        Properties properties = source.toProperties(true);
        for (String propertyName : properties.stringPropertyNames()) {
            merge.addProperty(propertyName, properties.getProperty(propertyName));
        }
    }

    @Override
    public int size() {
        return sourceList.size();
    }

    @Override
    public boolean isEmpty() {
        return sourceList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return sourceList.contains(o);
    }

    @Override
    public Iterator<Object> iterator() {
        return sourceList.iterator();
    }

    @Override
    public Object[] toArray() {
        return sourceList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return sourceList.toArray(a);
    }

    @Override
    public boolean add(Object o) {
        return sourceList.add(o);
    }

    @Override
    public boolean remove(Object o) {
        return sourceList.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return new HashSet<>(sourceList).containsAll(c);
    }

    @Override
    public boolean addAll(Collection<?> c) {
        return sourceList.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        return sourceList.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return sourceList.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return sourceList.retainAll(c);
    }

    @Override
    public void clear() {
        sourceList.clear();
    }

    @Override
    public Object get(int index) {
        return sourceList.get(index);
    }

    @Override
    public Object set(int index, Object element) {
        return sourceList.set(index, element);
    }

    @Override
    public void add(int index, Object element) {
        sourceList.add(index, element);
    }

    @Override
    public Object remove(int index) {
        return sourceList.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return sourceList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return sourceList.lastIndexOf(o);
    }

    @Override
    public ListIterator<Object> listIterator() {
        return sourceList.listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return sourceList.listIterator(index);
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return sourceList.subList(fromIndex, toIndex);
    }
}
