package com.luckyframework.httpclient.generalapi.ai.tool;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工具注册表：集中登记「工具声明」与其「执行实现」。
 * 注解生成、编程式登记、远端MCP工具清单三种来源进入注册表后是同一数据结构。
 * 提供全局默认实例，同时允许业务自建实例并显式传给 {@code @EnableToolCall}，不绑定IoC容器。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolRegistry {

    private static final ToolRegistry DEFAULT_REGISTRY = new ToolRegistry();

    /**
     * 全局默认实例
     */
    public static ToolRegistry getDefault() {
        return DEFAULT_REGISTRY;
    }

    private final Map<String, ToolSpec> specifications = new LinkedHashMap<>();
    private final Map<String, ToolExecutor> executors = new LinkedHashMap<>();

    /**
     * 只登记声明（无执行实现），此类工具可被列举并出现在报文里，被调用时返回「无执行实现」的失败结果
     */
    public synchronized ToolRegistry register(ToolSpec spec) {
        return register(spec, null, false);
    }

    /**
     * 登记声明与执行实现，同名冲突时抛异常
     */
    public synchronized ToolRegistry register(ToolSpec spec, ToolExecutor executor) {
        return register(spec, executor, false);
    }

    /**
     * 登记声明与执行实现
     *
     * @param overwrite 为true时允许覆盖同名登记，为false时同名登记抛 {@link ToolRegistryException}
     */
    public synchronized ToolRegistry register(ToolSpec spec, ToolExecutor executor, boolean overwrite) {
        if (spec == null) {
            throw new ToolRegistryException("Tool specification cannot be null.");
        }
        String name = spec.getName();
        if (!overwrite && specifications.containsKey(name)) {
            throw new ToolRegistryException("Duplicate tool name [" + name + "], "
                    + "it has already been registered, pass overwrite=true to replace it.");
        }
        specifications.put(name, spec);
        if (executor == null) {
            executors.remove(name);
        } else {
            executors.put(name, executor);
        }
        return this;
    }

    /**
     * 扫描对象上标注了{@link Tool}的方法，生成声明并把该方法绑定为执行实现
     *
     * @param toolHolder 工具宿主对象，声明按方法声明顺序登记
     */
    public synchronized ToolRegistry register(Object toolHolder) {
        return register(toolHolder, false);
    }

    /**
     * 扫描对象上标注了{@link Tool}的方法并登记
     *
     * @param overwrite 为true时允许覆盖同名登记
     */
    public synchronized ToolRegistry register(Object toolHolder, boolean overwrite) {
        if (toolHolder == null) {
            throw new ToolRegistryException("Tool holder cannot be null.");
        }
        for (Method method : toolHolder.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Tool.class)) {
                continue;
            }
            register(ToolSchemaGenerator.generateSpec(method), ToolMethodBinder.bind(toolHolder, method), overwrite);
        }
        return this;
    }

    /**
     * 按登记顺序列出全部工具声明
     */
    public synchronized List<ToolSpec> listSpecs() {
        return new ArrayList<>(specifications.values());
    }

    /**
     * 已登记的工具数量
     */
    public synchronized int size() {
        return specifications.size();
    }

    public synchronized boolean contains(String name) {
        return specifications.containsKey(name);
    }

    /**
     * 按名查询声明，未登记时返回空Optional而非null
     */
    public synchronized Optional<ToolSpec> findSpec(String name) {
        return Optional.ofNullable(specifications.get(name));
    }

    /**
     * 按名查询执行实现，未登记或仅有声明时返回空Optional
     */
    public synchronized Optional<ToolExecutor> findExecutor(String name) {
        return Optional.ofNullable(executors.get(name));
    }

    /**
     * 按名查询声明，未登记时抛出指明工具名的「未找到」异常
     */
    public synchronized ToolSpec getSpec(String name) {
        ToolSpec spec = specifications.get(name);
        if (spec == null) {
            throw new ToolRegistryException("Tool not found: " + name);
        }
        return spec;
    }

    /**
     * 构造只含部分工具的快照，原注册表不被修改
     */
    public synchronized ToolRegistry select(String... names) {
        return select(names == null ? Collections.<String>emptyList() : Arrays.asList(names));
    }

    /**
     * 构造只含部分工具的快照，原注册表不被修改
     */
    public synchronized ToolRegistry select(Collection<String> names) {
        ToolRegistry subset = new ToolRegistry();
        for (String name : names) {
            ToolSpec spec = specifications.get(name);
            if (spec == null) {
                throw new ToolRegistryException("Tool not found: " + name + ", it cannot be selected into a subset.");
            }
            subset.register(spec, executors.get(name));
        }
        return subset;
    }

    @Override
    public synchronized String toString() {
        return "ToolRegistry" + specifications.keySet();
    }
}
