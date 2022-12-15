package com.luckyframework.processor;

/**
 * 工厂Bean，用于生成一个Bean实例对象
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/4 09:03
 */
@FunctionalInterface
public interface FactoryBean<T> {

    T getBean();

}
