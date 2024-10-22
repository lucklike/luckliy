package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.spel.ParamWrapper;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Root对象为Map的{@link ParamWrapper}对象
 */
public class MapRootParamWrapper extends ParamWrapper {

    public MapRootParamWrapper() {
        initVariables();
    }

    public MapRootParamWrapper(MapRootParamWrapper paramWrapper) {
        super(paramWrapper);
    }

    private void initVariables() {
        setRootObject(new ConcurrentHashMap<>(16));
    }

    public void addRootVariable(String name, Object value) {
        getRootObject().put(name, value);
    }

    public void addRootVariables(Map<String, Object> rootVariables) {
        getRootObject().putAll(rootVariables);
    }

    public void addRootVariables(Method method, Object[] args) {
        getRootObject().putAll(getMethodArgsMap(method, args));
    }

    public void setRootVariables(Map<String, Object> rootVariables) {
        Map<String, Object> rootObject = getRootObject();
        rootObject.clear();
        rootObject.putAll(rootVariables);
    }

    public void removeRootVariable(String name) {
        getRootObject().remove(name);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRootObject() {
        return (Map<String, Object>) super.getRootObject();
    }

}
