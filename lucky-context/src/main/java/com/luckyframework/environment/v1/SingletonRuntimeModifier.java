package com.luckyframework.environment.v1;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.bean.factory.BeanReference;
import com.luckyframework.definition.PropertyValue;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;

/**
 * 单实例bean的运行时修改器的默认实现
 * @author fk7075
 * @version 1.0
 * @date 2021/11/14 9:36 上午
 */
public class SingletonRuntimeModifier implements RuntimeModifier {

    private final Environment environment;
    private final BeanFactory beanFactory;
    private final String targetBeanName;
    private final PropertyValue propertyValue;

    public SingletonRuntimeModifier(BeanFactory beanFactory, Environment environment, String targetBeanName, PropertyValue propertyValue) {
        this.environment = environment;
        this.beanFactory = beanFactory;
        this.targetBeanName = targetBeanName;
        this.propertyValue = propertyValue;
    }


    @Override
    public void setEnvironmentValue(String key, Object value) throws RuntimeModifierException {
        LuckyStandardEnvironment luckyMnv = (LuckyStandardEnvironment) environment;
        Object bean = beanFactory.getBean(targetBeanName);
        String fieldName = propertyValue.getName();
        BeanReference beanReference = ((BeanReference) propertyValue.getValue());
        String valueName = beanReference.getBeanName();
        if(ClassUtils.isCglibProxy(bean)){
            Object targetObject = ClassUtils.getCglibTargetObject(bean);
            Field field = FieldUtils.getDeclaredField(targetObject.getClass(), fieldName);
            FieldUtils.setValue(targetObject,field,luckyMnv.resolvePlaceholdersForType(valueName, ResolvableType.forField(field)));
        }else if(ClassUtils.isJDKProxy(bean)) {

        }
        FieldUtils.setValue(bean,propertyValue.getField(),luckyMnv.resolvePlaceholdersForType(valueName,beanReference.getType()));
    }
}
