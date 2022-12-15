package com.luckyframework.bean.factory;

/**
 * 泛型的工厂Bean
 * @author fk7075
 * @version 1.0
 * @date 2021/10/5 4:55 下午
 */
public interface GenericFactoryBean<T> {

    T getBean();
}
