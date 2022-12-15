package com.luckyframework.bean.factory;

@FunctionalInterface
public interface ObjectFactory<T> {

    T getObject();
}
