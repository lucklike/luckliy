package com.luckyframework.proxy.scope;

import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.definition.PropertyValue;
import com.luckyframework.definition.SetterValue;

/**
 * 不支持AOP代理，但是可以选择是否支持Scope代理BeanDefinition
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/21 21:29
 */
public class NonSupportAopScopeProxyBeanDefinition implements BeanDefinition {

    private final BeanDefinition targetBeanDefinition;

    public NonSupportAopScopeProxyBeanDefinition(BeanDefinition targetBeanDefinition) {
        this.targetBeanDefinition = targetBeanDefinition;
    }


    @Override
    public void setScope(BeanScopePojo scope) {
        targetBeanDefinition.setScope(scope);
    }

    @Override
    public boolean isSingleton() {
        return targetBeanDefinition.isSingleton();
    }

    @Override
    public boolean isPrototype() {
        return targetBeanDefinition.isPrototype();
    }

    @Override
    public BeanScopePojo getScope() {
        return targetBeanDefinition.getScope();
    }

    @Override
    public boolean isLazyInit() {
        return targetBeanDefinition.isLazyInit();
    }

    @Override
    public void setLazyInit(boolean lazyInit) {
        targetBeanDefinition.setLazyInit(lazyInit);
    }

    @Override
    public String[] getDependsOn() {
        return targetBeanDefinition.getDependsOn();
    }

    @Override
    public void setDependsOn(String[] depends) {
        targetBeanDefinition.setDependsOn(depends);
    }

    @Override
    public void setPrimary(boolean primary) {
        targetBeanDefinition.setPrimary(primary);
    }

    @Override
    public boolean isPrimary() {
        return targetBeanDefinition.isPrimary();
    }

    @Override
    public int getPriority() {
        return targetBeanDefinition.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        targetBeanDefinition.setPriority(priority);
    }

    @Override
    public FactoryBean getFactoryBean() {
        return targetBeanDefinition.getFactoryBean();
    }

    @Override
    public PropertyValue[] getPropertyValues() {
        return targetBeanDefinition.getPropertyValues();
    }

    @Override
    public void setPropertyValue(PropertyValue[] propertyValues) {
        targetBeanDefinition.setPropertyValue(propertyValues);
    }

    @Override
    public SetterValue[] getSetterValues() {
        return targetBeanDefinition.getSetterValues();
    }

    @Override
    public void setSetterValues(SetterValue[] setterValues) {
        targetBeanDefinition.setSetterValues(setterValues);
    }

    @Override
    public void setFactoryBean(FactoryBean factoryBean) {
        targetBeanDefinition.setFactoryBean(factoryBean);
    }

    @Override
    public void setInitMethodNames(String[] initMethodNames) {
        targetBeanDefinition.setInitMethodNames(initMethodNames);
    }

    @Override
    public String[] getInitMethodNames() {
        return targetBeanDefinition.getInitMethodNames();
    }

    @Override
    public void setDestroyMethodNames(String[] destroyMethodNames) {
        targetBeanDefinition.setDestroyMethodNames(destroyMethodNames);
    }

    @Override
    public String[] getDestroyMethodNames() {
        return targetBeanDefinition.getDestroyMethodNames();
    }

    @Override
    public boolean isPlugin() {
        return targetBeanDefinition.isPlugin();
    }

    @Override
    public void setAsPlugin() {
        targetBeanDefinition.setAsPlugin();
    }

    @Override
    public ProxyMode getProxyMode() {
        return targetBeanDefinition.getProxyMode();
    }

    @Override
    public void setProxyMode(ProxyMode proxyMode) {
        targetBeanDefinition.setProxyMode(proxyMode);
    }

    @Override
    public BeanDefinition copy() {
        return targetBeanDefinition.copy();
    }

    public BeanDefinition copyToCannotScopeProxyBeanDefinition() {
        return new NonSupportAopScopeProxyBeanDefinition(copy());
    }

    @Override
    public BeanDefinition getSuperBeanDefinition() {
        return targetBeanDefinition.getSuperBeanDefinition();
    }

    @Override
    public void setSuperBeanDefinition(BeanDefinition superBeanDefinition) {
        targetBeanDefinition.setSuperBeanDefinition(superBeanDefinition);
    }

    @Override
    public boolean isAvailable() {
        return targetBeanDefinition.isAvailable();
    }

    @Override
    public void setAvailable(boolean available) {
        targetBeanDefinition.setAvailable(available);
    }

    @Override
    public void setRole(int role) {
        targetBeanDefinition.setRole(role);
    }

    @Override
    public int getRole() {
        return targetBeanDefinition.getRole();
    }

    @Override
    public void setProxyDefinition(boolean isProxy) {
        targetBeanDefinition.setProxyDefinition(isProxy);
    }

    @Override
    public boolean isProxyDefinition() {
        return targetBeanDefinition.isProxyDefinition();
    }

    @Override
    public boolean isAutowireCandidate() {
        return targetBeanDefinition.isAutowireCandidate();
    }

    @Override
    public void setAutowireCandidate(boolean autowireCandidate) {
        targetBeanDefinition.setAutowireCandidate(autowireCandidate);
    }
}
