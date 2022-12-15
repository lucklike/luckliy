package com.luckyframework.bean.factory;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractObjectRegistry<T> implements ObjectRegistry<T>{

    private final List<T> objectList = new LinkedList<>();

    @Override
    public void registerObject(T object) {
        this.objectList.add(object);
    }

    @Override
    public List<T> getObjects() {
        return objectList;
    }
}
