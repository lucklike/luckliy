package com.luckyframework.environment;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.List;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/16 17:30
 */
interface EnvData {

    default List<Object> getEnvironmentValues(ObjectPropertySourcesPropertyResolver resolver, String propertyName){
        List<Object> values = new ArrayList<>();
        resolver.getPropertySources().forEach(ps -> values.addAll(getPropertySourceValues(resolver, ps, propertyName)));
        return values;
    }

    default List<Object> getPropertySourceValues(ObjectPropertySourcesPropertyResolver resolver, PropertySource<?> ps, String propertyName){
        List<Object> values = new ArrayList<>();
        if(ps instanceof CompositePropertySource){
            MutablePropertySources mps = ((CompositePropertySource) ps).getSource();
            for (PropertySource<?> mp : mps) {
                values.addAll(getPropertySourceValues(resolver, mp, propertyName));
            }
        }else{
            if(ps.containsProperty(propertyName)){
                Object property = resolver.requiredPlaceholderToObject(ps.getProperty(propertyName));
                values.add(property);
            }
        }
        return values;
    }

}
