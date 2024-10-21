package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.MutableMap;
import com.luckyframework.spel.ParamWrapper;
import com.sun.scenario.effect.Merge;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ProperSourcesParamWrapper extends ParamWrapper {

    private final AtomicBoolean init = new AtomicBoolean(false);

    public ProperSourcesParamWrapper() {
    }

    private void initVariables(String sourceName, MapRootParamWrapper mapRootParamWrapper) {

        // RootObject
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource(sourceName, mapRootParamWrapper.getRootObject()));

        // Variables
        MutableMap<String, Object> mutableMap = new MutableMap<>(false);
        mutableMap.addFirst(mapRootParamWrapper.getVariables());

        setRootObject(propertySources);
        setVariables(mutableMap);
    }

    private void coverVariables(String sourceName, MapRootParamWrapper mapRootParamWrapper) {
        if (init.compareAndSet(false, true)) {
            initVariables(sourceName, mapRootParamWrapper);
        } else {
            Map<String, Object> rootObject = mapRootParamWrapper.getRootObject();
            if (ContainerUtils.isNotEmptyMap(rootObject)) {
                getRootObject().addFirst(new MapPropertySource(sourceName, rootObject));
            }
            Map<String, Object> variables = mapRootParamWrapper.getVariables();
            if (ContainerUtils.isNotEmptyMap(variables)) {
                getVariables().addFirst(variables);
            }
        }
    }

    private void replenishVariables(String sourceName, MapRootParamWrapper mapRootParamWrapper) {
        if (init.compareAndSet(false, true)) {
            initVariables(sourceName, mapRootParamWrapper);
        } else {
            Map<String, Object> rootObject = mapRootParamWrapper.getRootObject();
            if (ContainerUtils.isNotEmptyMap(rootObject)) {
                getRootObject().addLast(new MapPropertySource(sourceName, rootObject));
            }
            Map<String, Object> variables = mapRootParamWrapper.getVariables();
            if (ContainerUtils.isNotEmptyMap(variables)) {
                getVariables().addLast(variables);
            }
        }
    }

    public void addRootVariable(String name, Object value) {
        ((MapPropertySource) getRootObject().stream().findFirst().get()).getSource().put(name, value);
    }

    public MutablePropertySources getRootObject() {
        return (MutablePropertySources) super.getRootObject();
    }

    public MutableMap<String, Object> getVariables() {
        return (MutableMap<String, Object>) super.getVariables();
    }

    public void coverMerge(String sourceName, MapRootParamWrapper mapRootParamWrapper) {
        importPackages(mapRootParamWrapper.getKnownPackagePrefixes());
        coverVariables(sourceName, mapRootParamWrapper);
    }

    public void replenishMerge(String sourceName, MapRootParamWrapper mapRootParamWrapper) {
        importPackages(mapRootParamWrapper.getKnownPackagePrefixes());
        replenishVariables(sourceName, mapRootParamWrapper);
    }
}
