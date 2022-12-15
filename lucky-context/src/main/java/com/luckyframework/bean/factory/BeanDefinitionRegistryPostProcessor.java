package com.luckyframework.bean.factory;

import com.luckyframework.definition.BeanDefinitionRegistry;

/**
 * Bean定义注册中心的后置处理器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/3/21 下午10:59
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor{

    void postProcessorBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry);
}
