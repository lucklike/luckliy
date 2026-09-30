package com.luckyframework.httpclient.core.util;

import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.luckyframework.httpclient.core.util.BeanUtils.copyProperties;

/**
 * 支持 SpEL 表达式的属性转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/5/7 01:08
 */
public class SpELPropertyCopyConvert implements PropertyConvert  {

    private static final Logger log = LoggerFactory.getLogger(SpELPropertyCopyConvert.class);

    private final PropertyFilter filter;
    private final Context context;

    /**
     * 已经处理过的对象与目标对象之间的对应关系（source -> target），
     * 用于在拷贝对象图时防止循环引用导致的无限递归
     */
    private final Map<Object, Object> visited = new IdentityHashMap<>();

    public SpELPropertyCopyConvert(Context context, PropertyFilter filter) {
        this.filter = filter;
        this.context = context;
    }


    @Override
    public void convert(PropertyInfo sourceProperty, PropertyInfo targetProperty) {
        // 进行SpEL计算
        Object propertyValue = sourceProperty.getValue();
        if (propertyValue instanceof String) {
            propertyValue = context.parseExpression(propertyValue.toString(), String.class);
        }

        if (sourceProperty.canDirectCopyType()) {
            targetProperty.setValue(propertyValue);
            return;
        }

        // 当前source对象此前已经处理过（循环引用或者对象共享），直接复用其对应的目标对象
        Object visitedTargetValue = propertyValue == null ? null : visited.get(propertyValue);
        if (visitedTargetValue != null) {
            targetProperty.setValue(visitedTargetValue);
            return;
        }

        Object targetPropertyValue = targetProperty.getValue();

        //目标对象的属性不为null时，直接进行属性的拷贝
        if (targetPropertyValue != null) {
            if (propertyValue != null) {
                visited.put(propertyValue, targetPropertyValue);
            }
            copyProperties(propertyValue, targetPropertyValue, filter, this);
        }
        // 目标对象的属性为null时，尝试使用反射调用其无参构造器进行构造之后再进行属性的拷贝
        else {
            try {
                Object newTargetPropertyValue = targetProperty.newObject();
                if (propertyValue != null) {
                    visited.put(propertyValue, newTargetPropertyValue);
                }
                copyProperties(propertyValue, newTargetPropertyValue, filter, this);
                targetProperty.setValue(newTargetPropertyValue);
            } catch (LuckyReflectionException e) {
                log.debug("Failed to create instance of type '{}' for property '{}', the property will be ignored", targetProperty.getType(), targetProperty.getName(), e);
            }
        }
    }
}
