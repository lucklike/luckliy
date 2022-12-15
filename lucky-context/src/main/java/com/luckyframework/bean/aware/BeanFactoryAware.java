package com.luckyframework.bean.aware;

import com.luckyframework.bean.factory.BeanFactory;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/6/28 下午11:20
 */
public interface BeanFactoryAware extends Aware{

    void setBeanFactory(BeanFactory beanFactory);
}
