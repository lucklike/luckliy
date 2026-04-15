package com.luckyframework.httpclient.core.util;

import com.luckyframework.conversion.JavaConversion;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.core.ResolvableType;

import java.beans.PropertyDescriptor;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 属性信息类
 */
public class FieldPropertyInfo implements PropertyInfo {

    /**
     * BeanWrapper
     */
    private final BeanWrapper wrapper;

    /**
     * 属性描述信息
     */
    private final PropertyDescriptor descriptor;

    /**
     * 属性类型
     */
    private final ResolvableType type;

    /**
     * 属性名
     */
    private final String name;

    /**
     * 属性值
     */
    private final Object value;

    /**
     * 对象提供者
     */
    private Supplier<Object> objectSupplier;

    /**
     * 构造函数
     *
     * @param wrapper    BeanWrapper
     * @param descriptor 属性描述信息
     */
    public FieldPropertyInfo(BeanWrapper wrapper, PropertyDescriptor descriptor) {
        this.wrapper = wrapper;
        this.descriptor = descriptor;
        this.type = Objects.requireNonNull(wrapper.getPropertyTypeDescriptor(descriptor.getName())).getResolvableType();
        this.name = descriptor.getName();
        this.value = isReadable() ? wrapper.getPropertyValue(name) : JavaConversion.getTypeDefaultValue(descriptor.getPropertyType());
    }

    /**
     * 查找某个属性
     *
     * @param name 属性名
     * @return 属性信息
     */
    public FieldPropertyInfo findProperty(String name) {
        try {
            return new FieldPropertyInfo(wrapper, wrapper.getPropertyDescriptor(name));
        } catch (Exception e) {
            return null;
        }
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
     * 创建一个新的属性对象
     *
     * @return 一个新的属性对象
     */
    @Override
    public Object newObject() {
        return ClassUtils.newObject(type.toClass());
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 获取属性值
     *
     * @return 属性值
     */
    @Override
    public Object getValue() {
        return value;
    }

    /**
     * 设置属性值
     *
     * @param value 值对象
     */
    @Override
    public void setValue(Object value) {
        wrapper.setPropertyValue(name, value);
    }

    @Override
    public ResolvableType getResolvableType() {
        return type;
    }

    /**
     * 当前属性是否可读
     *
     * @return 是否可读
     */
    @Override
    public boolean isReadable() {
        return wrapper.isReadableProperty(name);
    }

    /**
     * 当前属性是否可写
     *
     * @return 是否可写
     */
    @Override
    public boolean isWritable() {
        return wrapper.isWritableProperty(name);
    }

}
