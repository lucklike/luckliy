package com.luckyframework.bean.factory;

/**
 * Bean的后置处理器
 * @author fk
 * @version 1.0
 * @date 2021/3/16 0016 16:02
 */
public interface BeanPostProcessor {

    /** bean初始化前增强*/
    default Object postProcessBeforeInitialization(String beanName,FactoryBean factoryBean, Object bean) {
        return bean;
    }

    /** bean初始化后增强*/
    default Object postProcessAfterInitialization(String beanName,FactoryBean factoryBean,Object bean) {
        return bean;
    }
}
