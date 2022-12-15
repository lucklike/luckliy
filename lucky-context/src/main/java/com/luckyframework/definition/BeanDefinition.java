package com.luckyframework.definition;


import com.luckyframework.annotations.Configuration;
import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.proxy.scope.BeanScopePojo;
import com.luckyframework.bean.factory.FactoryBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/9 0009 11:48
 */
public interface BeanDefinition extends Cloneable , ResolvableTypeProvider {

    /** 扫描组件时得到的Bean*/
    int SCANNER_BEAN = 1;

    /** 由配置类产出的Bean*/
    int CONFIG_METHOD_BEAN = 2;

    /** 内部使用的Bean*/
    int INTERNAL_USE_BEAN = 3;

    /** 通过SPI机制导入的Bean*/
    int SPI_BEAN = 4;

    /** 通过@Import注解导入的Bean*/
    int IMPORT_BEAN = 5;

    /** 插件Bean*/
    int PLUGIN_BEAN = 6;

    /** 真实的临时Bean*/
    int TARGET_TEMP_BEAN = 7;

    /** {@link Configuration @Configuration} 注解标注的Bean*/
    int CONFIGURATION_BEAN = 8;

    /** 设置Bean的作用域 */
    void setScope(BeanScopePojo scope);

    /** 是否为单例 */
    boolean isSingleton();

    /** 是否为原型 */
    boolean isPrototype();

    BeanScopePojo getScope();

    /** 是否延迟初始化 */
    boolean isLazyInit();

    /** 设置是否延迟初始化 */
    void setLazyInit(boolean lazyInit);

    /** 设置依赖bean的名称 */
    String[] getDependsOn();

    /** 返回依赖bean的名称 */
    void setDependsOn(String[] depends);

    /** 设置是否优先匹配 */
    void setPrimary(boolean primary);

    /** 是否优先匹配 */
    boolean isPrimary();

    /** 获取优先级*/
    int getPriority();

    /** 设置优先级 */
    void setPriority(int priority);

    /** 获取工厂Bean*/
    FactoryBean getFactoryBean();

    /** 属性依赖 */
    PropertyValue[] getPropertyValues();

    /** 设置属性依赖*/
    void setPropertyValue(PropertyValue[] propertyValues);

    /** 方法依赖*/
    SetterValue[] getSetterValues();

    /** 设置方法依赖*/
    void setSetterValues(SetterValue[] setterValues);

    /** 设置工厂Bean*/
    void setFactoryBean(FactoryBean factoryBean);

    /** 设置初始化方法 */
    void setInitMethodNames(String[] initMethodNames);

    /** 获取初始化方法 */
    String[] getInitMethodNames();

    /** 设置销毁方法 */
    void setDestroyMethodNames(String[] destroyMethodNames);

    /** 获取销毁方法 */
    String[] getDestroyMethodNames();

    /** 是否为插件*/
    boolean isPlugin();

    /** 设置为插件*/
    void setAsPlugin();

    /** 返回代理模型 */
    ProxyMode getProxyMode();

    /** 设置代理模型*/
    void setProxyMode(ProxyMode proxyMode);

    /** 获取Bean的类型 */
    default ResolvableType getResolvableType(){
        return getFactoryBean().getResolvableType();
    }

    /** 复制 */
    BeanDefinition copy();

    /** 获取父BeanDefinition*/
    BeanDefinition getSuperBeanDefinition();

    /** 设置父BeanDefinition*/
    void setSuperBeanDefinition(BeanDefinition superBeanDefinition);

    /** 是否可用*/
    boolean isAvailable();

    /** 设置可用性*/
    void setAvailable(boolean available);

    /** 设置角色*/
    void setRole(int role);

    /** 获取角色信息*/
    int getRole();

    void setProxyDefinition(boolean isProxy);

    boolean isProxyDefinition();

    boolean isAutowireCandidate();

    void setAutowireCandidate(boolean autowireCandidate);

}
