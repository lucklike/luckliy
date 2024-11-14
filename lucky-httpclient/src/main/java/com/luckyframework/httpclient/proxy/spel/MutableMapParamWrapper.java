package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.MutableMap;
import com.luckyframework.spel.ParamWrapper;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MutableMapParamWrapper extends ParamWrapper {

    private final AtomicBoolean init = new AtomicBoolean(false);

    public MutableMapParamWrapper() {
    }

    private void initVariables(MapRootParamWrapper mapRootParamWrapper) {

        // RootObject
        MutableMap<String, Object> propertySources = new MutableMap<>();
        propertySources.addFirst(mapRootParamWrapper.getRootObject());

        // Variables
        MutableMap<String, Object> mutableMap = new MutableMap<>(false);
        mutableMap.addFirst(mapRootParamWrapper.getVariables());

        setRootObject(propertySources);
        setVariables(mutableMap);
    }

    private void coverVariables(MapRootParamWrapper mapRootParamWrapper) {
        if (init.compareAndSet(false, true)) {
            initVariables(mapRootParamWrapper);
        } else {
            Map<String, Object> rootObject = mapRootParamWrapper.getRootObject();
            if (ContainerUtils.isNotEmptyMap(rootObject)) {
                getRootObject().addFirst(rootObject);
            }
            Map<String, Object> variables = mapRootParamWrapper.getVariables();
            if (ContainerUtils.isNotEmptyMap(variables)) {
                getVariables().addFirst(variables);
            }
        }
    }

    private void replenishVariables(MapRootParamWrapper mapRootParamWrapper) {
        if (init.compareAndSet(false, true)) {
            initVariables(mapRootParamWrapper);
        } else {
            Map<String, Object> rootObject = mapRootParamWrapper.getRootObject();
            if (ContainerUtils.isNotEmptyMap(rootObject)) {
                getRootObject().addLast(rootObject);
            }
            Map<String, Object> variables = mapRootParamWrapper.getVariables();
            if (ContainerUtils.isNotEmptyMap(variables)) {
                getVariables().addLast(variables);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public MutableMap<String, Object> getRootObject() {
        return (MutableMap<String, Object>) super.getRootObject();
    }

    public MutableMap<String, Object> getVariables() {
        return (MutableMap<String, Object>) super.getVariables();
    }

    public void coverMerge(MapRootParamWrapper mapRootParamWrapper) {
        importPackages(mapRootParamWrapper.getKnownPackagePrefixes());
        coverVariables(mapRootParamWrapper);
    }

    public void replenishMerge(MapRootParamWrapper mapRootParamWrapper) {
        importPackages(mapRootParamWrapper.getKnownPackagePrefixes());
        replenishVariables(mapRootParamWrapper);
    }
}
