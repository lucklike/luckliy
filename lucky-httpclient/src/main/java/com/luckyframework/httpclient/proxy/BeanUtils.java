package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.ContainerUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * BeanUtils
 */
public class BeanUtils {

    public static <T> void copyPropertiesIgnoreNonInitValue(T source, T target) {
        copyProperties(source, target, getAllNonInitValuePropertyNames(target));
    }

    @SuppressWarnings("unchecked")
    public static <T> void copyProperties(T source, T target, String... ignoreProperties) {
        if (source == null || target == null) {
            return;
        }
        if ((source instanceof Map) && (target instanceof Map)) {
            Object[] ignoreKeys = new Object[ignoreProperties.length];
            System.arraycopy(ignoreProperties, 0, ignoreKeys, 0, ignoreKeys.length);
            copyMap((Map<Object, Object>) source, (Map<Object, Object>) target, ignoreKeys);
        } else {
            org.springframework.beans.BeanUtils.copyProperties(source, target, ignoreProperties);
        }
    }

    public static void copyMap(Map<Object, Object> source, Map<Object, Object> target, Object... ignoreProperties) {
        Set<Object> ignoreKeys = ContainerUtils.arrayToSet(ignoreProperties);
        source.forEach((k, v) -> {
            if (!ignoreKeys.contains(k)) {
                target.put(k, v);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static String[] getAllNonInitValuePropertyNames(Object object) {
        if (object == null) {
            return new String[0];
        }
        if (object instanceof Map) {
            return getAllNonNullPropertyNamesByMap((Map<Object, Object>) object);
        }
        return getAllNonInitValuePropertyNamesByBean(object);
    }

    private static String[] getAllNonInitValuePropertyNamesByBean(Object bean) {
        List<String> ignorePropertieList = new ArrayList<>();
        BeanWrapper targetWrapper = new BeanWrapperImpl(bean);
        for (PropertyDescriptor descriptor : targetWrapper.getPropertyDescriptors()) {

            // 属性值为null的属性
            Object propertyValue = targetWrapper.getPropertyValue(descriptor.getName());
            if (propertyValue == null) {
                continue;
            }

            // 基本类型，且值等于初始值
            Class<?> propertyType = descriptor.getPropertyType();
            if (propertyType.isPrimitive() && !Objects.equals(propertyValue, getPrimitiveTypeDefaultValue(propertyType))) {
                continue;
            }

            ignorePropertieList.add(descriptor.getName());
        }
        return ignorePropertieList.toArray(new String[0]);
    }

    private static String[] getAllNonNullPropertyNamesByMap(Map<Object, Object> map) {
        List<String> ignorePropertieList = new ArrayList<>();
        map.forEach((k, v) -> {
            if (v != null) {
                ignorePropertieList.add(String.valueOf(k));
            }
        });
        return ignorePropertieList.toArray(new String[0]);
    }

    public static Object getPrimitiveTypeDefaultValue(Class<?> type) {
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\u0000';
        if (type == boolean.class) return false;
        return null;
    }
}
