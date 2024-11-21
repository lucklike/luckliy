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

    private void initVariables(SpELVariate spELVariate) {

        // RootObject
        MutableMap<String, Object> rootVar = new MutableMap<>();
        rootVar.addFirst(spELVariate.getRoot());

        // Variables
        MutableMap<String, Object> varMap = new MutableMap<>(false);
        varMap.addFirst(spELVariate.getVar());

        setRootObject(rootVar);
        setVariables(varMap);
    }

    private void coverVariables(SpELVariate spELVariate) {
        if (init.compareAndSet(false, true)) {
            initVariables(spELVariate);
        } else {
            Map<String, Object> rootObject = spELVariate.getRoot();
            if (ContainerUtils.isNotEmptyMap(rootObject)) {
                getRootObject().addFirst(rootObject);
            }
            Map<String, Object> variables = spELVariate.getVar();
            if (ContainerUtils.isNotEmptyMap(variables)) {
                getVariables().addFirst(variables);
            }
        }
    }

    private void replenishVariables(SpELVariate spELVariate) {
        if (init.compareAndSet(false, true)) {
            initVariables(spELVariate);
        } else {
            Map<String, Object> rootVar = spELVariate.getRoot();
            if (ContainerUtils.isNotEmptyMap(rootVar)) {
                getRootObject().addLast(rootVar);
            }
            Map<String, Object> varMap = spELVariate.getVar();
            if (ContainerUtils.isNotEmptyMap(varMap)) {
                getVariables().addLast(varMap);
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
        importPackages(spELVariate.getPacks());
        coverVariables(spELVariate);
    }

    public void replenishMerge(SpELVariate spELVariate) {
        importPackages(spELVariate.getPacks());
        replenishVariables(spELVariate);
    }
}
