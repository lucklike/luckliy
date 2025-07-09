package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.MutableMap;
import com.luckyframework.spel.ParamWrapper;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MutableMapParamWrapper extends ParamWrapper {

    private final AtomicBoolean init = new AtomicBoolean(false);

    public MutableMapParamWrapper() {
    }

    private void initVariables(SpELVariate spELVariate) {

        // RootObject
        MutableMap<String, Object> rootVar = new MutableMap<>();
        rootVar.addFirst(unmodifiableMap(spELVariate.getRoot()));

        // Variables
        MutableMap<String, Object> varMap = new MutableMap<>(false);
        varMap.addFirst(unmodifiableMap(spELVariate.getVar()));

        setRootObject(rootVar);
        setVariables(varMap);
    }

    private void coverVariables(SpELVariate spELVariate) {
        if (init.compareAndSet(false, true)) {
            initVariables(spELVariate);
        } else {
            Map<String, Object> rootObject = spELVariate.getRoot();
            if (ContainerUtils.isNotEmptyMap(rootObject)) {
                getRootObject().addFirst(unmodifiableMap(rootObject));
            }
            Map<String, Object> variables = spELVariate.getVar();
            if (ContainerUtils.isNotEmptyMap(variables)) {
                getVariables().addFirst(unmodifiableMap(variables));
            }
        }
    }

    private void replenishVariables(SpELVariate spELVariate) {
        if (init.compareAndSet(false, true)) {
            initVariables(spELVariate);
        } else {
            Map<String, Object> rootVar = spELVariate.getRoot();
            if (ContainerUtils.isNotEmptyMap(rootVar)) {
                getRootObject().addLast(unmodifiableMap(rootVar));
            }
            Map<String, Object> varMap = spELVariate.getVar();
            if (ContainerUtils.isNotEmptyMap(varMap)) {
                getVariables().addLast(unmodifiableMap(varMap));
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

    public void coverMerge(SpELVariate spELVariate) {
        getTypeLocator().merge(spELVariate.getTypeLocator());
        coverVariables(spELVariate);
    }

    public void replenishMerge(SpELVariate spELVariate) {
        getTypeLocator().merge(spELVariate.getTypeLocator());
        replenishVariables(spELVariate);
    }

    private Map<String, Object> unmodifiableMap(Map<String, Object> sourceMap) {
        return Collections.unmodifiableMap(sourceMap);
    }
}
