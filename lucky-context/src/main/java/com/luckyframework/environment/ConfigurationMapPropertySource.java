package com.luckyframework.environment;

import com.luckyframework.common.ConfigurationMap;
import org.springframework.core.env.PropertySource;

import java.util.Map;

/**
 * 基于{@link ConfigurationMap}实现的PropertySource
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/11 14:31
 */
public class ConfigurationMapPropertySource extends PropertySource<ConfigurationMap> {

    public ConfigurationMapPropertySource(String name, Map<String, Object> map) {
        super(name, new ConfigurationMap(map));
    }

    public ConfigurationMapPropertySource(String name, ConfigurationMap source) {
        super(name, source);
    }

    public ConfigurationMapPropertySource(String name) {
        super(name);
    }

    @Override
    public Object getProperty(String name) {
        Object value = source.get(name);
        if (value != null) {
            return value;
        }
        return containsProperty(name) ? source.getProperty(name) : null;
    }

    @Override
    public boolean containsProperty(String name) {
        return source.containsConfigKey(name);
    }

    public boolean isEmpty() {
        return source.isEmpty();
    }
}
