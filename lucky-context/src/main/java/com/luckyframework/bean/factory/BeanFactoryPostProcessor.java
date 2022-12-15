package com.luckyframework.bean.factory;

import com.luckyframework.definition.BeanDefinitionRegistry;

import static com.luckyframework.definition.BeanDefinition.TARGET_TEMP_BEAN;

/**
 * BeanFactory的后置处理器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/3/21 下午10:59
 */
public interface BeanFactoryPostProcessor {

    String TEMP_BEAN_NAME_PREFIX = "@";


    void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory);

    static boolean isTempTargetBeanName(BeanDefinitionRegistry definitionRegistry, String beanName){
        return beanName.startsWith(TEMP_BEAN_NAME_PREFIX) && TARGET_TEMP_BEAN == definitionRegistry.getBeanDefinition(beanName).getRole();
    }
}
