package com.luckyframework.bean.factory;

/**
 * 代理BeanFactory
 */
public interface AgentBeanFactory {

    BeanFactory getTargetBeanFactory();
}
