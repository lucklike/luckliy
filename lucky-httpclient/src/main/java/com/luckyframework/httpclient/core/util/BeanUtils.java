package com.luckyframework.httpclient.core.util;

import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.lang.NonNull;

import java.beans.PropertyDescriptor;
import java.util.Objects;

/**
 * BeanUtils
 */
public abstract class BeanUtils {

    /**
     * 属性拷贝, 如果target对象中的某个属性不为初始值时（引用类型的初始值为null， 基本类型的初始值参考JDK规范），拷贝时则忽略该属性
     *
     * @param source 源对象
     * @param target 目标对象
     * @param <T>    类型泛型
     */
    public static <T> void copyPropertiesIgnoreNonInitValue(T source, T target) {
        TargetPropertyIsDefValueExecuteCopy filter = new TargetPropertyIsDefValueExecuteCopy();
        copyProperties(source, target, filter, new DefaultPropertyConvert(filter));
    }

    /**
     * 属性拷贝
     *
     * @param source 源对象
     * @param target 目标对象
     * @param <T>    类型泛型
     */
    public static <T> void copyProperties(T source, T target) {
        DefaultPropertyFilter filter = new DefaultPropertyFilter();
        copyProperties(source, target, filter, new DefaultPropertyConvert(filter));
    }


    /**
     * 属性拷贝
     *
     * @param source 源对象
     * @param target 目标对象
     * @param filter 属性过滤器
     * @param <T>    类型泛型
     */
    public static <T> void copyProperties(T source, T target, @NonNull PropertyFilter filter) {
        copyProperties(source, target, filter, new DefaultPropertyConvert(filter));
    }


    /**
     * 属性拷贝
     *
     * @param source  源对象
     * @param target  目标对象
     * @param filter  属性过滤器
     * @param convert 属性转换器
     * @param <T>     类型泛型
     */
    public static <T> void copyProperties(T source, T target, @NonNull PropertyFilter filter, @NonNull PropertyConvert convert) {
        // 有一个为null时直接结束拷贝
        if (source == null || target == null) {
            return;
        }

        // 生成BeanWrapper
        BeanWrapper sourceWrapper = new BeanWrapperImpl(source);
        BeanWrapper targetWrapper = new BeanWrapperImpl(target);

        // 执行转换逻辑
        for (PropertyDescriptor descriptor : targetWrapper.getPropertyDescriptors()) {
            PropertyInfo sourcePropertyInfo = new PropertyInfo(sourceWrapper, descriptor);
            PropertyInfo targetPropertyInfo = new PropertyInfo(targetWrapper, descriptor);
            if (filter.needConvert(sourcePropertyInfo, targetPropertyInfo)) {
                convert.convert(sourcePropertyInfo, targetPropertyInfo);
            }
        }
    }

    /**
     * 默认的属性转换器
     */
    static class DefaultPropertyConvert implements PropertyConvert {

        private final PropertyFilter filter;

        DefaultPropertyConvert(PropertyFilter filter) {
            this.filter = filter;
        }


        @Override
        public void convert(PropertyInfo sourceProperty, PropertyInfo targetProperty) {
            if (sourceProperty.isJdkType()) {
                targetProperty.setValue(sourceProperty.getValue());
            } else {
                Object targetPropertyValue = targetProperty.getValue();

                //目标对象的属性不为null时，直接进行属性的拷贝
                if (targetPropertyValue != null) {
                    copyProperties(sourceProperty.getValue(), targetPropertyValue, filter, this);
                }
                // 目标对象的属性为null时，尝试使用反射调用其无参构造器进行构造之后再进行属性的拷贝
                else {
                    try {
                        Object newTargetPropertyValue = ClassUtils.newObject(targetProperty.getDescriptor().getPropertyType());
                        copyProperties(sourceProperty.getValue(), newTargetPropertyValue, filter, this);
                        targetProperty.setValue(newTargetPropertyValue);
                    } catch (LuckyReflectionException e) {
                        // ignore
                    }
                }


            }
        }
    }

    /**
     * 默认的属性过滤器
     */
    static class DefaultPropertyFilter implements PropertyFilter {

        @Override
        public boolean needConvert(PropertyInfo sourceProperty, PropertyInfo targetProperty) {
            return canEditor(sourceProperty, targetProperty) && notEqual(sourceProperty, targetProperty);
        }

        protected boolean canEditor(PropertyInfo sourceProperty, PropertyInfo targetProperty) {
            return sourceProperty.isReadable() && targetProperty.isWritable();
        }

        protected boolean notEqual(PropertyInfo sourceProperty, PropertyInfo targetProperty) {
            return !Objects.equals(sourceProperty.getValue(), targetProperty.getValue());
        }

    }

    /**
     * 真实对象属性为默认值时才进行属性拷贝
     */
    static class TargetPropertyIsDefValueExecuteCopy extends DefaultPropertyFilter {

        @Override
        public boolean needConvert(PropertyInfo sourceProperty, PropertyInfo targetProperty) {
            if (targetProperty.isJdkType()) {
                return canEditor(sourceProperty, targetProperty) && targetProperty.isDefaultValue();
            }

            return super.needConvert(sourceProperty, targetProperty);
        }
    }

}
