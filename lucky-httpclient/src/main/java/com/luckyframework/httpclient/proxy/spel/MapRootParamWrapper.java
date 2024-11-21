package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.CtrlMap;
import com.luckyframework.spel.ParamWrapper;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Root对象为Map的{@link ParamWrapper}对象
 */
public class MapRootParamWrapper extends ParamWrapper {

    private static final CtrlMap.ModifiedVerifier<String>  ERR_VERIFIER = k -> !InternalParamName.getAllInternalParamName().contains(k);
    private static final CtrlMap.ModifiedVerifier<String>  IGNORE_VERIFIER = k -> !ProhibitCoverEnum.isMatch(k);

    public MapRootParamWrapper() {
        super(new CtrlMap<>(new ConcurrentHashMap<>(64), ERR_VERIFIER, IGNORE_VERIFIER));
        initVariables();
    }

    public MapRootParamWrapper(MapRootParamWrapper paramWrapper) {
        super(paramWrapper);
    }

    private void initVariables() {
        setRootObject(new CtrlMap<>(new ConcurrentHashMap<>(64), ERR_VERIFIER, IGNORE_VERIFIER));
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
