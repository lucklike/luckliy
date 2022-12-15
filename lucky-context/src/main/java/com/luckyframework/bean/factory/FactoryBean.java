package com.luckyframework.bean.factory;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

/**
 * 工厂Bean，用来创建一个Bean的实例
 * @author fk7075
 * @version 1.0.0
 * @date 2021/6/28 下午10:50
 */
public interface FactoryBean extends ResolvableTypeProvider {

    /**
     * 创建一个Bean的实例
     * @return bean的实例
     */
    Object createBean();

    /**
     * 创建该bean所依赖的其他类型
     * @return 该bean所依赖的其他类型
     */
    default ResolvableType[] createDependOnTypes(){
        return null;
    }
}
