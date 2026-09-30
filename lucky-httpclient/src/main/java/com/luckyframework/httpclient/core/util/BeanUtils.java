package com.luckyframework.httpclient.core.util;

import com.luckyframework.exception.LuckyReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.lang.NonNull;

import java.beans.PropertyDescriptor;
import java.util.IdentityHashMap;
import java.util.Map;
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
            FieldPropertyInfo sourceFieldPropertyInfo = new FieldPropertyInfo(sourceWrapper, descriptor);
            FieldPropertyInfo targetFieldPropertyInfo = new FieldPropertyInfo(targetWrapper, descriptor);
            if (filter.needConvert(sourceFieldPropertyInfo, targetFieldPropertyInfo)) {
                convert.convert(sourceFieldPropertyInfo, targetFieldPropertyInfo);
            }
        }
    }

    /**
     * 默认的属性转换器
     */
    public static class DefaultPropertyConvert implements PropertyConvert {

        private static final Logger log = LoggerFactory.getLogger(DefaultPropertyConvert.class);

        private final PropertyFilter filter;

        /**
         * 已经处理过的对象与目标对象之间的对应关系（source -> target），
         * 用于在拷贝对象图时防止循环引用导致的无限递归
         */
        private final Map<Object, Object> visited = new IdentityHashMap<>();

        DefaultPropertyConvert(PropertyFilter filter) {
            this.filter = filter;
        }


        @Override
        public void convert(PropertyInfo sourceProperty, PropertyInfo targetProperty) {
            if (sourceProperty.canDirectCopyType()) {
                targetProperty.setValue(sourceProperty.getValue());
                return;
            }

            Object sourcePropertyValue = sourceProperty.getValue();

            // 当前source对象此前已经处理过（循环引用或者对象共享），直接复用其对应的目标对象
            Object visitedTargetValue = sourcePropertyValue == null ? null : visited.get(sourcePropertyValue);
            if (visitedTargetValue != null) {
                targetProperty.setValue(visitedTargetValue);
                return;
            }

            Object targetPropertyValue = targetProperty.getValue();

            //目标对象的属性不为null时，直接进行属性的拷贝
            if (targetPropertyValue != null) {
                if (sourcePropertyValue != null) {
                    visited.put(sourcePropertyValue, targetPropertyValue);
                }
                copyProperties(sourcePropertyValue, targetPropertyValue, filter, this);
            }
            // 目标对象的属性为null时，尝试使用反射调用其无参构造器进行构造之后再进行属性的拷贝
            else {
                try {
                    Object newTargetPropertyValue = targetProperty.newObject();
                    if (sourcePropertyValue != null) {
                        visited.put(sourcePropertyValue, newTargetPropertyValue);
                    }
                    copyProperties(sourcePropertyValue, newTargetPropertyValue, filter, this);
                    targetProperty.setValue(newTargetPropertyValue);
                } catch (LuckyReflectionException e) {
                    log.debug("Failed to create instance of type '{}' for property '{}', the property will be ignored", targetProperty.getType(), targetProperty.getName(), e);
                }
            }
        }
    }

    /**
     * 默认的属性过滤器
     */
    public static class DefaultPropertyFilter implements PropertyFilter {

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
    public static class TargetPropertyIsDefValueExecuteCopy extends DefaultPropertyFilter {

        @Override
        public boolean needConvert(PropertyInfo sourceProperty, PropertyInfo targetProperty) {
            if (targetProperty.isValueType()) {
                return canEditor(sourceProperty, targetProperty) && targetProperty.isDefaultValue();
            }

            return super.needConvert(sourceProperty, targetProperty);
        }
    }

}
