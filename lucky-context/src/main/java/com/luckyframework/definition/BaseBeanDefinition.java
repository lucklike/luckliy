package com.luckyframework.definition;

import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.proxy.scope.BeanScopePojo;
import com.luckyframework.bean.factory.FactoryBean;
import org.springframework.core.Ordered;

/**
 * 最基本的Bean定义信息
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/4 下午3:08
 */
public class BaseBeanDefinition implements BeanDefinition {

    //单例或者多例
    private BeanScopePojo scope = BeanScopePojo.DEF_SINGLETON;
    //是否懒加载
    private boolean lazyInit = false;
    //是否设置为优先注入
    private boolean isPrimary = false;
    //初始化方法
    private String[] initMethodNames = new String[0];
    //销毁方法 -> 只针对与单例bean有效
    private String[] destroyMethodNames = new String[0];
    //依赖与其他bean，这里是依赖bean的beanName
    private String[] dependsOn = new String[0];
    //初始化优先级
    private int priority = Ordered.LOWEST_PRECEDENCE;
    //工厂Bean
    private FactoryBean factoryBean;
    //属性依赖
    private PropertyValue[] propertyValues = new PropertyValue[0];
    // Setter方法属性依赖
    private SetterValue[] setterValues = new SetterValue[0];
    // 父BeanDefinition
    private BeanDefinition superBeanDefinition = null;
    // 可用性描述
    private boolean isAvailable = true;
    //是否为插件
    private boolean isPlugin = false;
    //代理模型
    private ProxyMode proxyMode = ProxyMode.AUTO;
    // Bean的类型
    private int role = SCANNER_BEAN;
    // 是否是代理定义
    private boolean isProxyDefinition = false;
    /** 是否允许按类型自动装配*/
    private boolean autowireCandidate = true;

    @Override
    public void setScope(BeanScopePojo scope) {
        this.scope = scope;
        this.proxyMode = scope.getProxyMode();
    }

    @Override
    public boolean isSingleton() {
        return this.scope.isSingleton();
    }

    @Override
    public boolean isPrototype() {
        return this.scope.isPrototype();
    }

    @Override
    public BeanScopePojo getScope() {
        return this.scope;
    }

    @Override
    public boolean isLazyInit() {
        return this.lazyInit;
    }

    @Override
    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    @Override
    public String[] getDependsOn() {
        return this.dependsOn;
    }

    @Override
    public void setDependsOn(String[] depends) {
        this.dependsOn = depends;
    }

    @Override
    public void setPrimary(boolean primary) {
        this.isPrimary = primary;
    }

    @Override
    public boolean isPrimary() {
        return this.isPrimary;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public FactoryBean getFactoryBean() {
        return this.factoryBean;
    }

    @Override
    public PropertyValue[] getPropertyValues() {
        return this.propertyValues;
    }

    @Override
    public void setPropertyValue(PropertyValue[] propertyValues) {
        this.propertyValues = propertyValues;
    }

    @Override
    public SetterValue[] getSetterValues() {
        return this.setterValues;
    }

    @Override
    public void setSetterValues(SetterValue[] setterValues) {
        this.setterValues = setterValues;
    }

    @Override
    public void setFactoryBean(FactoryBean factoryBean) {
        this.factoryBean = factoryBean;
    }

    @Override
    public void setInitMethodNames(String[] initMethodNames) {
        this.initMethodNames = initMethodNames;
    }

    @Override
    public String[] getInitMethodNames() {
        return this.initMethodNames;
    }

    @Override
    public void setDestroyMethodNames(String[] destroyMethodNames) {
        this.destroyMethodNames = destroyMethodNames;
    }

    @Override
    public String[] getDestroyMethodNames() {
        return this.destroyMethodNames;
    }

    @Override
    public boolean isPlugin() {
        return this.isPlugin;
    }

    @Override
    public void setAsPlugin() {
        this.isPlugin = true;
    }

    @Override
    public ProxyMode getProxyMode() {
        return this.proxyMode;
    }

    @Override
    public void setProxyMode(ProxyMode proxyModel) {
        this.proxyMode = proxyModel;
    }

    public BeanDefinition copy() {
        try {
            return (BeanDefinition) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BeanDefinition getSuperBeanDefinition() {
        return this.superBeanDefinition;
    }

    @Override
    public void setSuperBeanDefinition(BeanDefinition superBeanDefinition) {
        this.superBeanDefinition = superBeanDefinition;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    @Override
    public int getRole() {
        return role;
    }

    @Override
    public void setRole(int role) {
        this.role = role;
    }

    @Override
    public boolean isProxyDefinition() {
        return isProxyDefinition;
    }

    @Override
    public void setProxyDefinition(boolean proxyDefinition) {
        isProxyDefinition = proxyDefinition;
    }


    @Override
    public boolean isAutowireCandidate() {
        return this.autowireCandidate;
    }

    @Override
    public void setAutowireCandidate(boolean autowireCandidate) {
        this.autowireCandidate = autowireCandidate;
    }
}
