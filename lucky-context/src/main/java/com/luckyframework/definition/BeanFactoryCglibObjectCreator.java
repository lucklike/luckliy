package com.luckyframework.definition;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.exception.CglibObjectCreatorException;
import com.luckyframework.proxy.CglibObjectCreator;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.core.env.Environment;

import java.lang.reflect.Constructor;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/5/15 06:37
 */
public class BeanFactoryCglibObjectCreator implements CglibObjectCreator {

    private final Class<?> targetClass;
    private final BeanFactory beanFactory;
    private final Environment environment;

    public BeanFactoryCglibObjectCreator(Class<?> targetClass, BeanFactory beanFactory, Environment environment) {
        this.targetClass = targetClass;
        this.beanFactory = beanFactory;
        this.environment = environment;
    }

    @Override
    public Object createProxyObject(Enhancer enhancer) {
        try {
            if(targetClass.isInterface()){
                return enhancer.create();
            }else{
                Constructor<?> constructor = ClassUtils.findConstructor(targetClass);
                Class<?>[] parameterTypes = ClassUtils.findConstructorParameterTypes(constructor);
                Object[] parameterValue = BeanReferenceUtils.getMayBeLazyRealParameterValues(beanFactory, environment, ClassUtils.findConstructorBeanReferenceParameters(constructor));
                return  enhancer.create(parameterTypes, parameterValue);
            }
        }catch (Exception e){
            throw new CglibObjectCreatorException("Failed to create Cglib proxy object for '"+targetClass+"' class.",e);
        }
    }

}
