package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.context.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class HookManager {

    private final Map<Class<?>, HookGroup> hooks = new LinkedHashMap<>(32);

    public void addHookGroup(String namespace, Class<?> hookClass) {
        hooks.computeIfAbsent(hookClass, _k -> HookGroup.create(namespace, hookClass));
    }

    public void addHookGroup(Class<?> hookClass) {
        hooks.computeIfAbsent(hookClass, _k -> HookGroup.create(hookClass));
    }

    public void useHook(Lifecycle lifecycle, Context context) {
        hooks.values().forEach(h -> h.useHook(lifecycle, context));
    }


}
