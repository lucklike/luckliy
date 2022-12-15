package com.luckyframework.annotations;

import com.luckyframework.bean.factory.ListableBeanFactory;
import com.luckyframework.definition.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * 条件上下文
 * @author fk
 * @version 1.0
 * @date 2021/3/26 0026 14:39
 */
public interface ConditionContext {

    BeanDefinitionRegistry getRegistry();

    Environment getEnvironment();

    @Nullable
    ListableBeanFactory getBeanFactory();

    ResourceLoader getResourceLoader();

    @Nullable
    ClassLoader getClassLoader();
}
