package com.luckyframework.bean.factory;

import com.luckyframework.definition.BeanDefinitionRegistry;
import com.luckyframework.proxy.scope.ScopeRegistry;
import org.springframework.core.env.Environment;

import java.util.function.Consumer;

/**
 * 全能型的Bean工厂
 */
public interface VersatileBeanFactory extends ListableBeanFactory, PluginManager, ScopeRegistry,BeanDefinitionRegistry,BeanPostProcessorRegistry {

    /**
     * 获取Lucky的环境变量
     * @return 获取环境变量
     */
    Environment getEnvironment();

    /**
     * 得到按优先级排序后所有单例Bean的名称
     * @return
     */
    String[] prioritizedSingletonBeans();

    /**
     * 如何为Aware接口设置对应值的逻辑
     * @param invokeAwareMethodConsumer 为Aware接口设置值逻辑
     */
    void setInvokeAwareMethodConsumer(Consumer<Object> invokeAwareMethodConsumer);

    /**
     *  清理容器
     */
    void clear();
}
