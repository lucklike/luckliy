package com.luckyframework.bean.factory;

import com.luckyframework.common.TempPair;
import org.springframework.core.ResolvableType;

/**
 * 函数式工厂Bean接口
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 11:08 上午
 */
@FunctionalInterface
public interface FunctionalFactoryBean extends FactoryBean{

    TempPair<Object,ResolvableType> beanInfo();

    default Object createBean() {
        return beanInfo().getOne();
    }

    @Override
    default ResolvableType getResolvableType() {
        return beanInfo().getTwo();
    }

}
