package com.luckyframework.proxy.scope;

import com.luckyframework.bean.factory.ObjectFactory;

public interface Scope {

    Object get(String beanName, ObjectFactory<?> objectFactory);

    Object remove(String beanName);

}
