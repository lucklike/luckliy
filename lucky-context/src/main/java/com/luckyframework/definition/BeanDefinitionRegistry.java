package com.luckyframework.definition;

import com.luckyframework.exception.BeanDefinitionRegisterException;

import java.util.List;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/12 0012 18:28
 */
public interface BeanDefinitionRegistry {

    /** 注册bean定义*/
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionRegisterException;

    /** 获取bean定义*/
    BeanDefinition getBeanDefinition(String beanName);

    /** 判断是否包含bean定义*/
    boolean containsBeanDefinition(String beanName);

    /** 移除bean的定义*/
    void removeBeanDefinition(String beanName);

    /** 获取Bean定义信息的总数*/
    int getBeanDefinitionCount();

    /** 获取所有bean定义的注册名*/
    String[] getBeanDefinitionNames();

    /** 获取所有的Bean定义信息*/
    List<BeanDefinition> getBeanDefinitions();

}
