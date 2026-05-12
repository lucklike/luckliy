package com.luckyframework.httpclient.core.util;

import com.luckyframework.exception.LuckyReflectionException;
import com.luckyframework.httpclient.proxy.context.Context;

import static com.luckyframework.httpclient.core.util.BeanUtils.copyProperties;

/**
 * 支持 SpEL 表达式的属性转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/5/7 01:08
 */
public class SpELPropertyCopyConvert implements PropertyConvert  {

    private final PropertyFilter filter;
    private final Context context;

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

        if (sourceProperty.isJdkType()) {
            targetProperty.setValue(propertyValue);
        } else {
            Object targetPropertyValue = targetProperty.getValue();

            //目标对象的属性不为null时，直接进行属性的拷贝
            if (targetPropertyValue != null) {
                copyProperties(propertyValue, targetPropertyValue, filter, this);
            }
            // 目标对象的属性为null时，尝试使用反射调用其无参构造器进行构造之后再进行属性的拷贝
            else {
                try {
                    Object newTargetPropertyValue =targetProperty.newObject();
                    copyProperties(propertyValue, newTargetPropertyValue, filter, this);
                    targetProperty.setValue(newTargetPropertyValue);
                } catch (LuckyReflectionException e) {
                    // ignore
                }
            }


        }
    }
}
