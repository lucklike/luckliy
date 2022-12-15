package com.luckyframework.environment.v1;


import com.luckyframework.annotations.PropertySource;
import com.luckyframework.annotations.PropertySources;
import com.luckyframework.configuration.ConfigurationReader;
import com.luckyframework.exception.EnvironmentSettingException;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.type.AnnotationMetadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于@PropertySource注解的环境变量
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/25 下午11:00
 */
public class PropertySourceEnvironment extends AbstractEnvironment {



    private final StorageUnit dataUnit;

    public PropertySourceEnvironment(List<AnnotationMetadata> componentAnnotationMetadata){
        Map<String,Object> propertySourceMap = new ConcurrentHashMap<>();
        Set<PropertySource> propertySourceSet = findPropertySource(componentAnnotationMetadata);
        for (PropertySource propertySource : propertySourceSet) {
            propertySourceMap.putAll(new ConfigurationReader(propertySource).getResourceData());
        }
        dataUnit = new ConfigurationMapStorageUnit(propertySourceMap);
    }

    private Set<PropertySource> findPropertySource(List<AnnotationMetadata> componentAnnotationMetadata){
        Set<PropertySource> sources = new HashSet<>();
        String propertySourceClassName = PropertySource.class.getName();
        String propertySourcesClassName = PropertySources.class.getName();
        for (AnnotationMetadata component : componentAnnotationMetadata) {
            Set<String> containsAnnotations = component.getAnnotationTypes();
            if(containsAnnotations.contains(propertySourceClassName)||containsAnnotations.contains(propertySourcesClassName)){
                List<PropertySource> propertySources = AnnotationUtils.strengthenGet(ClassUtils.getClass(component.getClassName()), PropertySource.class);
                List<PropertySources> propertySourcesList = AnnotationUtils.strengthenGet(ClassUtils.getClass(component.getClassName()), PropertySources.class);
                sources.addAll(propertySources);
                propertySourcesList.forEach(p->sources.addAll(Arrays.asList(p.value())));
            }
        }
        return sources;
    }

    @Override
    public Object getProperty(String key) {
        return dataUnit.getRealValue(key);
    }

    @Override
    public Object parsSingleExpression(String single$Expression) {
        return dataUnit.parsSingleExpression(single$Expression);
    }

    @Override
    public Object parsExpression(Object $Expression) {
        return dataUnit.parsExpression($Expression);
    }

    @Override
    public void setProperty(String key, Object value) {
        throw new EnvironmentSettingException("Unable to set property source environment variable.");
    }

    @Override
    public Map<String, Object> getProperties() {
        return dataUnit.getRealMap();
    }

    @Override
    public Map<String, Object> getOriginalMap() {
        return dataUnit.getOriginalMap();
    }

    @Override
    public boolean containsKey(String key) {
        return getOriginalMap().containsKey(key);
    }
}
