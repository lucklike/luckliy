package com.luckyframework.aop;


import com.luckyframework.aop.proxy.ProxyFactory;
import com.luckyframework.context.ApplicationContext;

/**
 * 自动代理创建器
 * @author fk
 * @version 1.0
 * @date 2021/4/13 0013 11:13
 */
public class AutoProxyAdvisorCreator extends AbstractAopAutoProxyAdvisorCreator {

    @Override
    public ProxyFactory getProxyFactory(ApplicationContext applicationContext, String beanName, Object target) {
        ProxyFactory proxyFactory = new ProxyFactory(applicationContext, beanName, target);
        proxyFactory.setEnableGlobalCglibProxy(enableGlobalCglibProxy);
        return proxyFactory;
    }
}
