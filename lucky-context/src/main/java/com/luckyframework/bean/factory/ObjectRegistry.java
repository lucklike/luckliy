package com.luckyframework.bean.factory;

import java.util.List;

public interface ObjectRegistry<T> {

    /**
     * 注册一个对象
     */
    void registerObject(T object);

    /**
     * 获取所有注册的对象
     */
    List<T> getObjects();
}
