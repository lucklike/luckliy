package com.luckyframework.annotations;

import com.luckyframework.bean.factory.ListableBeanFactory;
import com.luckyframework.definition.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * 条件上下文的具体实现
 * @author fk
 * @version 1.0
 * @date 2021/3/26 0026 14:47
 */
public class ConditionContextImpl implements ConditionContext {

    private final BeanDefinitionRegistry registry;
    private final Environment environment;
    private final ClassLoader loader;
    private final ListableBeanFactory beanFactory;
    private final ResourceLoader resourceLoader;

    public ConditionContextImpl(BeanDefinitionRegistry registry, Environment environment, ListableBeanFactory beanFactory, ResourceLoader resourceLoader) {
        this.registry = registry;
        this.environment = environment;
        this.beanFactory = beanFactory;
        this.resourceLoader = resourceLoader;
        this.loader = resourceLoader.getClassLoader();
    }

    @Override
    public BeanDefinitionRegistry getRegistry() {
        return this.registry;
    }

    @Override
    public Environment getEnvironment() {
        return this.environment;
    }

    @Nullable
    @Override
    public ListableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    @Nullable
    @Override
    public ClassLoader getClassLoader() {
        return this.loader;
    }
}
