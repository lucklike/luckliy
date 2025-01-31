package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.common.ContainerUtils;

import java.util.List;

/**
 * 代理装饰器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/28 01:25
 */
public class ProxyDecorator {

    /**
     * 所有代理插件集合
     */
    private final List<ProxyPlugin> plugins;

    /**
     * 执行器元数据
     */
    private final ExecuteMeta meta;

    /**
     * 当前正在执行的插件索引
     */
    private int index;

    /**
     * 代理装饰器构造器
     *
     * @param plugins 插件集合
     * @param meta    执行器元数据
     */
    public ProxyDecorator(List<ProxyPlugin> plugins, ExecuteMeta meta) {
        this.plugins = plugins;
        this.meta = meta;
        this.plugins.forEach(plugin -> plugin.init(this.meta));
    }

    /**
     * 获取执行器元数据
     *
     * @return 执行器元数据
     */
    public ExecuteMeta getMeta() {
        return meta;
    }

    /**
     * 执行当前正在运行的方法
     *
     * @return 执行结果
     * @throws Throwable 执行过程中可能产生的异常
     */
    public Object proceed() throws Throwable {
        if (ContainerUtils.isEmptyCollection(plugins) || index >= plugins.size()) {
            return meta.proceed();
        }
        ProxyPlugin plugin = plugins.get(index++);
        return plugin.decorate(this);
    }


    /**
     * 使用指定的参数来执行当前正在运行的方法
     *
     * @param args 方法参数列表
     * @return 执行结果
     * @throws Throwable 执行过程中可能产生的异常
     */
    public Object proceed(Object... args) throws Throwable {
        meta.setArgs(args);
        return proceed();
    }

}
