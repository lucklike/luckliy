package com.luckyframework.definition;

import com.luckyframework.reflect.FieldUtils;

import java.lang.reflect.Field;

/**
 * 属性参数
 * @author fk7075
 * @version 1.0.0
 * @date 2021/3/21 下午11:58
 */
public class PropertyValue {

    /** 属性名*/
    private final String fieldName;
    /** 属性值*/
    private final Object fieldValue;
    /** 属性*/
    private Field field;
    /** 是否需要懒加载*/
    private boolean isLazy = false;

    public PropertyValue(String name, Object value) {
        this(name,value,null);
    }

    public PropertyValue(String fieldName, Object fieldValue, Field field) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.field = field;
    }

    public String getName() {
        return fieldName;
    }

    public Object getValue() {
        return fieldValue;
    }

    public Field getField() {
        return field;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    public Field getField(Class<?> aClass){
        if(field == null){
            field = FieldUtils.getDeclaredField(aClass,fieldName);
        }
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }
}
