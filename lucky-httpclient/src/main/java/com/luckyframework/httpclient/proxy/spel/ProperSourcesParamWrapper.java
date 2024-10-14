package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.spel.ParamWrapper;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProperSourcesParamWrapper extends ParamWrapper {

    private final String ROOT_SOURCE_NAME = "RootSource";

    public ProperSourcesParamWrapper() {
        initRootObject();
    }

    public ProperSourcesParamWrapper(String expression) {
        super(expression);
        initRootObject();
    }

    private void initRootObject() {
        initRootObject(new ConcurrentHashMap<>(16));
    }

    private void initRootObject(Map<String, Object> rootMap) {
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addLast(new MapPropertySource(ROOT_SOURCE_NAME, rootMap));
        setRootObject(propertySources);
    }

    public void addRootVariable(String name, Object value) {
        ((MapPropertySource) getRootObject().get(ROOT_SOURCE_NAME)).getSource().put(name, value);
    }

    public void addRootVariables(Map<String, Object> rootVariables) {
        ((MapPropertySource) getRootObject().get(ROOT_SOURCE_NAME)).getSource().putAll(rootVariables);
    }

    public void addRootVariables(Method method, Object[] args)  {
        addRootVariables(getMethodArgsMap(method, args));
    }

    public void setRootVariables(Map<String, Object> rootVariables) {
        initRootObject(rootVariables);
    }

    public void removeRootVariable(String name) {
        getRootObject().remove(name);
    }

    public MutablePropertySources getRootObject() {
        return (MutablePropertySources) super.getRootObject();
    }

    public void mergeVar(MapRootParamWrapper mapRootParamWrapper) {
        importPackages(mapRootParamWrapper.getKnownPackagePrefixes());
        addRootVariables(mapRootParamWrapper.getRootObject());
        addVariables(mapRootParamWrapper.getVariables());
    }
}
