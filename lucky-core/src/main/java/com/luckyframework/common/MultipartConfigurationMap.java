package com.luckyframework.common;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/17 5:50 下午
 */
public class MultipartConfigurationMap implements SupportsStringManipulationMap{

    public MultipartConfigurationMap(@NonNull List<ConfigurationMap> multipartMapList){
        Assert.notNull(multipartMapList,"multipartMapList is not null!");
    }


    @Override
    public Object getProperty(String configKey) {
        return null;
    }

    @Override
    public SupportsStringManipulationMap getMap(String configKey) {
        return null;
    }

    @Override
    public List<? extends SupportsStringManipulationMap> getMapList(String configKey) {
        return null;
    }

    @Override
    public void addProperty(String configKey, Object confValue) {

    }

    @Override
    public void addProperties(Map<?, ?> properties) {

    }

    @Override
    public void addAsItExists(String configKey, Object confValue) {

    }

    @Override
    public void addAsItExists(Map<String, Object> properties) {

    }

    @Override
    public boolean containsConfigKey(String configKey) {
        return false;
    }

    @Override
    public Object removeConfigProperty(String configKey) {
        return null;
    }

    @Override
    public void mergeConfig(Map<String, Object> newConfigMap) {

    }

    @Override
    public Object putIn(String sourceKey, String newKey, Object newValue) {
        return null;
    }

    @Override
    public void putEntity(Object entity) {

    }
}
