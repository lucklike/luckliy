package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.StaticClassEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/12 01:32
 */
public class SpELImportConf {

    private Map<String, Object> root = new LinkedHashMap<>();
    private Map<String, Object> val= new LinkedHashMap<>();
    private List<Class<?>> fun = new ArrayList<>();
    private List<String> pack = new ArrayList<>();

    public Map<String, Object> getRoot() {
        return root;
    }

    public void setRoot(Map<String, Object> root) {
        this.root = root;
    }

    public Map<String, Object> getVal() {
        return val;
    }

    public void setVal(Map<String, Object> val) {
        this.val = val;
    }

    public List<Class<?>> getFun() {
        return fun;
    }

    public void setFun(List<Class<?>> fun) {
        this.fun = fun;
    }

    public List<String> getPack() {
        return pack;
    }

    public void setPack(List<String> pack) {
        this.pack = pack;
    }

    public void importSpELRuntime(Context context) {
        MapRootParamWrapper contextVar = context.getContextVar();
        for (Class<?> fun : fun) {
            StaticClassEntry classEntry = StaticClassEntry.create(fun);
            contextVar.addVariables(classEntry.getAllStaticMethods());
        }

        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String key = context.parseExpression(entry.getKey(), String.class);
            Object value = getParsedValue(context, entry.getValue());
            contextVar.addRootVariable(key, value);
        }

        for (Map.Entry<String, Object> entry : val.entrySet()) {
            String key = context.parseExpression(entry.getKey(), String.class);
            Object value = getParsedValue(context, entry.getValue());
            contextVar.addVariable(key, value);
        }
        contextVar.importPackage(pack.toArray(new String[0]));
    }


    private Object getParsedValue(Context context, Object value) {
        if (ContainerUtils.isIterable(value)) {
            List<Object> list = new ArrayList<>();
            for (Object object : ContainerUtils.getIterable(value)) {
                list.add(getParsedValue(context, object));
            }
            return list;
        }
        if (value instanceof Map) {
            Map<?, ?> valueMap = (Map<?, ?>) value;
            Map<String, Object> map = new LinkedHashMap<>(valueMap.size());
            for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
                String key = context.parseExpression(String.valueOf(entry.getKey()), String.class);
                map.put(key, getParsedValue(context, entry.getValue()));
            }
            return map;
        }
        if (value instanceof String) {
            return context.parseExpression(String.valueOf(value) , Object.class);
        }
        return value;
    }
}
