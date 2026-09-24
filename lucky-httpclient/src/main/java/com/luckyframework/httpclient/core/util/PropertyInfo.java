package com.luckyframework.httpclient.core.util;

import com.luckyframework.conversion.JavaConversion;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.ResolvableType;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 属性信息类
 */
public interface PropertyInfo {

    /**
     * 创建一个新的属性对象
     *
     * @return 一个新的属性对象
     */
    Object newObject();

    /**
     * 获取属性名
     *
     * @return 属性名
     */
    String getName();

    /**
     * 获取属性值
     *
     * @return 属性值
     */
    Object getValue();

    /**
     * 设置属性值
     *
     * @param value 值对象
     */
    void setValue(Object value);

    /**
     * 获取属性的类型
     *
     * @return 属性类型
     */
    ResolvableType getResolvableType();

    /**
     * 获取属性最外层的Class
     *
     * @return 属性最外层的Class
     */
    default Class<?> getType() {
        return getResolvableType().toClass();
    }

    /**
     * 当前属性值是否为null
     *
     * @return 是否为null
     */
    default boolean isNullValue() {
        return getValue() == null;
    }

    /**
     * 当前属性类型是否为基本类型
     *
     * @return 是否为基本类型
     */
    default boolean isPrimitive() {
        return getType().isPrimitive();
    }

    /**
     * 当前值是否为该类型的默认值
     *
     * @return 是否为该类型的默认值
     */
    default boolean isDefaultValue() {
        return isPrimitive()
                ? Objects.equals(JavaConversion.getTypeDefaultValue(getType()), getValue())
                : isNullValue();
    }

    /**
     * 当前属性是否为JDK中的类型
     *
     * @return 否为JDK中的类型
     */
    default boolean isJdkType() {
        return ClassUtils.isSimpleBaseType(getType());
    }

    /**
     * 当前属性是否可以直接进行拷贝赋值
     *
     * @return 当前属性是否可以直接进行拷贝赋值
     */
    default boolean canDirectCopyType() {
        return ClassUtils.isValueType(getType()) || Map.class.isAssignableFrom(getType()) || Collection.class.isAssignableFrom(getType());
    }

    /**
     * 当前属性是否为值类型
     * <p>值类型包括：JDK简单类型(参考{@link ClassUtils#isSimpleBaseType(Class)})、枚举类型、数组类型、
     * Class、UUID以及java.time包下的日期时间等不可变类型，
     * 值类型的属性在拷贝时直接进行引用赋值，不进行深度拷贝
     *
     * @return 当前属性是否为值类型
     */
    default boolean isValueType() {
        return ClassUtils.isValueType(getType());
    }

    /**
     * 当前属性是否为简单基本类型
     *
     * @return 是否为简单基本类型
     */
    default boolean isSimpleBaseType() {
        return ClassUtils.isSimpleBaseType(getType());
    }

    /**
     * 当前属性是否可读
     *
     * @return 是否可读
     */
    default boolean isReadable() {
        return true;
    }

    /**
     * 当前属性是否可写
     *
     * @return 是否可写
     */
    default boolean isWritable() {
        return true;
    }

}
