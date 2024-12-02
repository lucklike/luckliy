package com.luckyframework.httpclient.proxy.spel.hook;

import com.luckyframework.httpclient.proxy.context.Context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hook管理器，用于管理一组Class中的所有Hook
 */
public class HookManager {

    /**
     * Class和与子对应的Hook组所组成的Map
     */
    private final Map<Class<?>, HookGroup> hooks = new LinkedHashMap<>(32);

    /**
     * 添加一个Hook组
     *
     * @param namespace 命名空间
     * @param hookClass Hook类的Class
     */
    public void addHookGroup(String namespace, Class<?> hookClass) {
        hooks.computeIfAbsent(hookClass, _k -> HookGroup.create(namespace, hookClass));
    }

    /**
     * 添加一个Hook组，使用默认的命名空间
     *
     * @param hookClass Hook类的Class
     */
    public void addHookGroup(Class<?> hookClass) {
        hooks.computeIfAbsent(hookClass, _k -> HookGroup.create(hookClass));
    }

    /**
     * 运行指定生命周期下的所有Hook函数
     *
     * @param lifecycle 生命周期
     * @param context   上下文对象
     */
    public void useHook(Lifecycle lifecycle, Context context) {
        hooks.values().forEach(h -> h.useHook(lifecycle, context));
    }


}
