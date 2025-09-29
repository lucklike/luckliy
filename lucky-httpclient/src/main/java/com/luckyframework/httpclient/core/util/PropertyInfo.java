package com.luckyframework.httpclient.core.util;

import com.luckyframework.conversion.JavaConversion;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.beans.BeanWrapper;

import java.beans.PropertyDescriptor;
import java.util.Objects;

/**
 * 属性信息类
 */
public class PropertyInfo {

    /**
     * BeanWrapper
     */
    private final BeanWrapper wrapper;

    /**
     * 属性描述信息
     */
    private final PropertyDescriptor descriptor;

    /**
     * 属性名
     */
    private final String name;

    /**
     * 属性值
     */
    private final Object value;


    /**
     * 构造函数
     *
     * @param wrapper    BeanWrapper
     * @param descriptor 属性描述信息
     */
    public PropertyInfo(BeanWrapper wrapper, PropertyDescriptor descriptor) {
        this.wrapper = wrapper;
        this.descriptor = descriptor;
        this.name = descriptor.getName();
        this.value = isReadable() ? wrapper.getPropertyValue(name) : JavaConversion.getTypeDefaultValue(descriptor.getPropertyType());
    }

    /**
     * 获取BeanWrapper
     *
     * @return BeanWrapper
     */
    public BeanWrapper getWrapper() {
        return wrapper;
    }

    /**
     * 获取属性描述信息
     *
     * @return 属性描述信息
     */
    public PropertyDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * 获取属性值
     *
     * @return 属性值
     */
    public Object getValue() {
        return value;
    }

    /**
     * 设置属性值
     *
     * @param value 值对象
     */
    public void setValue(Object value) {
        wrapper.setPropertyValue(name, value);
    }

    /**
     * 当前属性值是否为null
     *
     * @return 是否为null
     */
    public boolean isNullValue() {
        return value == null;
    }

    /**
     * 当前属性类型是否为基本类型
     *
     * @return 是否为基本类型
     */
    public boolean isPrimitive() {
        return descriptor.getPropertyType().isPrimitive();
    }

    /**
     * 当前值是否为该类型的默认值
     *
     * @return 是否为该类型的默认值
     */
    public boolean isDefaultValue() {
        return isPrimitive()
                ? Objects.equals(JavaConversion.getTypeDefaultValue(descriptor.getPropertyType()), value)
                : isNullValue();
    }

    /**
     * 当前属性是否为JDK中的类型
     *
     * @return 否为JDK中的类型
     */
    public boolean isJdkType() {
        return ClassUtils.isJdkType(descriptor.getPropertyType());
    }

    /**
     * 当前属性是否可读
     *
     * @return 是否可读
     */
    public boolean isReadable() {
        return wrapper.isReadableProperty(name);
    }

    /**
     * 当前属性是否可写
     *
     * @return 是否可写
     */
    public boolean isWritable() {
        return wrapper.isWritableProperty(name);
    }

}
