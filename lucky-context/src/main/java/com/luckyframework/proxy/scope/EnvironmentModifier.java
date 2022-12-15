package com.luckyframework.proxy.scope;

import com.luckyframework.bean.aware.ApplicationEventPublisherAware;
import com.luckyframework.bean.aware.EnvironmentAware;
import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.context.event.ApplicationEventPublisher;
import com.luckyframework.environment.ConfigurationMapPropertySource;
import com.luckyframework.environment.LuckyStandardEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/30 11:02
 */
public class EnvironmentModifier implements ApplicationEventPublisherAware, EnvironmentAware {

    private final String REFRESH_SOURCE_NAME = "refreshSource";

    private ApplicationEventPublisher eventPublisher;

    private Environment environment;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setProperty(String propName, Object value){
        Map<String, Object> map = new HashMap<>();
        map.put(propName, value);
        setProperties(map);
    }

    public void setProperties(Map<String, Object> propNameValueMap){
        if(!ContainerUtils.isEmptyMap(propNameValueMap)){
            MutablePropertySources mps = ((LuckyStandardEnvironment) environment).getPropertySources();
            if(!mps.contains(REFRESH_SOURCE_NAME)){
                ConfigurationMap configMap = new ConfigurationMap();
                configMap.addConfigProperties(propNameValueMap);
                ConfigurationMapPropertySource configMapSource = new ConfigurationMapPropertySource(REFRESH_SOURCE_NAME, configMap);
                mps.addFirst(configMapSource);
            }else {
                ConfigurationMapPropertySource configMapSource = (ConfigurationMapPropertySource) mps.get(REFRESH_SOURCE_NAME);
                configMapSource.getSource().addConfigProperties(propNameValueMap);
            }
        }
    }


    public void  setPropertyRefreshAll(String propName, Object value){
        Map<String, Object> map = new HashMap<>();
        map.put(propName, value);
        setPropertiesRefreshAll(map);
    }

    public void  setPropertiesRefreshAll(Map<String, Object> propNameValues){
        if(!ContainerUtils.isEmptyMap(propNameValues)){
            setProperties(propNameValues);
            refreshAll();
        }
    }

    public void refreshAll(){
        eventPublisher.publishEvent(EnvRefreshedEvent.RELOAD_ALL);
    }



}
