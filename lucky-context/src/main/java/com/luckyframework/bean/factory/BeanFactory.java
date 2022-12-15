package com.luckyframework.bean.factory;

import com.luckyframework.exception.BeansException;
import com.luckyframework.exception.NoSuchBeanDefinitionException;
import org.springframework.core.ResolvableType;

import java.io.Closeable;

/**
 * Bean工厂
 * @author fk7075
 * @version 1.0.0
 * @date 2021/3/21 下午10:59
 */
public interface BeanFactory extends Closeable {

    String[] EMPTY_STRING_ARRAY = new String[0];

    NullObject NULL_BEAN = new NullObject();

    /**
     * 获取bean的类型,但是不创建对象
     * @param name bean的名称
     * @return bean的类型
     */
    Class<?> getType(String name)throws BeansException;

    /**
     * 获取bean的类型
     * @param beanName bean的名称
     * @return bean的类型
     */
    ResolvableType getResolvableType(String beanName);

    /**
     * 获取一个bean的实例
     * @param name bean的唯一ID
     * @return bean实例
     * @throws BeansException bean异常
     */
    Object getBean(String name) throws BeansException;

    /**
     * 获取一个bean实例，并将其转化为对应的类型
     * @param name bean的唯一ID
     * @param requiredType bean的类型，可以是接口或者抽象类
     * @return bean实例
     */
    <T> T getBean(String name,Class<T> requiredType) throws BeansException;

    /**
     * 根据bean的类型来获得实例
     * @param requiredType bean的类型
     * @return bean实例
     */
    <T> T getBean(Class<T> requiredType) throws BeansException;

    /**
     * 根据bean的类型来获得实例
     * @param requiredType bean的类型
     * @return bean实例
     */
    <T> T getBean(ResolvableType requiredType) throws BeansException;

    /**
     * 使用新的构造器参数创建一个bean实例
     * @param name bean的名称
     * @param args 构造器参数
     * @return bean实例
     * @throws BeansException 如法创建该bean实例时
     */
    Object getBean(String name, Object... args) throws BeansException;

    /**
     * 使用新的构造器参数创建一个bean实例
     * @param requiredType bean的类型
     * @param args 构造器参数
     * @param <T> 具体类型
     * @return bean实例
     */
    <T> T getBean(Class<T> requiredType,Object...args) throws BeansException;

    /**
     * 使用新的构造器参数创建一个bean实例
     * @param requiredType bean的类型
     * @param args 构造器参数
     * @param <T> 具体类型
     * @return bean实例
     */
    <T> T getBean(ResolvableType requiredType,Object...args) throws BeansException;

    /**
     * 类型检查，检查指定的bean名称的bean实例是否与给定的类型兼容
     * @param name bean的名称
     * @param typeToMatch 待检查的类型
     * @return
     * @throws NoSuchBeanDefinitionException 找不到bean实例时抛出该异常
     */
    boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

    /**
     * 类型检查，检查指定的bean名称的bean实例是否与给定的类型兼容
     * @param name bean的名称
     * @param typeToMatch 待检查的类型
     * @return
     * @throws NoSuchBeanDefinitionException 找不到bean实例时抛出该异常
     */
    boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

    /**
     * 是否包含该名称的bean实例
     * @param name 待检验的bean实例名称
     * @return
     */
    boolean containsBean(String name);

    /**
     * 是否是单例bean
     * @param name bean的名称
     * @return 是否为单例
     * @throws NoSuchBeanDefinitionException
     */
    boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

    /**
     * 是否是原型bean
     * @param name bean的名称
     * @return 是否为原型
     * @throws NoSuchBeanDefinitionException
     */
    boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

    void invokeAwareMethod(Object instance);

}
