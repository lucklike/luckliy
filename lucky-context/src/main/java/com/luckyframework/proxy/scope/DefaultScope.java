package com.luckyframework.proxy.scope;

import com.luckyframework.bean.factory.ObjectFactory;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/21 05:34
 */
public class DefaultScope implements Scope {
    @Override
    public Object get(String beanName, ObjectFactory<?> objectFactory) {
        return objectFactory.getObject();
    }

    @Override
    public Object remove(String beanName) {
        return null;
    }
}
