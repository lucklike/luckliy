package com.luckyframework.definition;

import com.luckyframework.bean.factory.ConstructorFactoryBean;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

/**
 * Bean定义的建造者
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/15 下午7:16
 */
public final class BeanDefinitionBuilder {

    private BeanDefinitionBuilder(){}

    /**
     * 使用{@link FactoryBean}和{@link AnnotatedTypeMetadata}构造一个bean定义信息
     * @param factory 工厂Bean
     * @param beanAnnotatedTypeMetadata 该bea对应的AnnotatedTypeMetadata
     * @return
     */
    public static GenericBeanDefinition builder(@NonNull FactoryBean factory, AnnotatedTypeMetadata beanAnnotatedTypeMetadata){
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setFactoryBean(factory);
        definition.setBeanDefinitionField(beanAnnotatedTypeMetadata);
        if(beanAnnotatedTypeMetadata instanceof ClassMetadata){
            Class<?> introspectedClass = ClassUtils.getClass(((ClassMetadata)beanAnnotatedTypeMetadata).getClassName());
            definition.setPropertyValue(definition.getPropertyValues(introspectedClass));
            definition.setSetterValues(definition.getSetterValues(introspectedClass));
        }
        return definition;
    }


    /**
     * 构造一个Bean的定义信息,使用ConstructorFactoryBean
     * @param beanClass beanClass
     * @param args 构造器参数列表
     */
    public static GenericBeanDefinition builderByConstructor(@NonNull Class<?> beanClass, Object[] args){
        return builder(new ConstructorFactoryBean(beanClass,args),AnnotationMetadata.introspect(beanClass));
    }


    /**
     * 构造一个Bean的定义信息,使用ConstructorFactoryBean
     * @param beanClassName beanClass的全类名
     */
    public static GenericBeanDefinition builderByConstructor(@NonNull String beanClassName){
        return new GenericBeanDefinition(beanClassName);
    }

    /**
     * 构造一个Bean的定义信息,使用ConstructorFactoryBean
     * @param beanClass bean的Class
     */
    public static GenericBeanDefinition builderByConstructor(@NonNull Class<?> beanClass){
        return new GenericBeanDefinition(beanClass);
    }

    /**
     * 构造一个Bean的定义信息,使用MethodFactoryBean
     * @param beanName      bean实例的名称
     * @param factoryMethod 工厂方法的实例
     */
    public static GenericBeanDefinition builderByFactoryMethod(@NonNull String beanName,@NonNull Method factoryMethod){
        return new GenericBeanDefinition(beanName,factoryMethod);
    }

    /**
     * 构造一个Bean的定义信息,使用StaticMethodFactoryBean
     * @param beanClass             bean的Class
     * @param staticFactoryMethod   工厂方法的实例
     */
    public static GenericBeanDefinition builderByStaticFactoryMethod(@NonNull Class<?> beanClass,@NonNull Method staticFactoryMethod){
        return new GenericBeanDefinition(beanClass,staticFactoryMethod);
    }

    /**
     * 构造一个Bean的定义信息,使用StaticMethodFactoryBean
     * @param beanClassName             beanClass的全类名
     * @param staticFactoryMethodName   工厂方法名称
     */
    public static GenericBeanDefinition builderByStaticFactoryMethod(@NonNull String beanClassName,@NonNull String staticFactoryMethodName){
        return new GenericBeanDefinition(beanClassName,staticFactoryMethodName);
    }

    /**
     * 构造一个Bean的定义信息,使用StaticMethodFactoryBean
     * @param beanClass                 bean的Class
     * @param staticFactoryMethodName   工厂方法名称
     */
    public static GenericBeanDefinition builderByStaticFactoryMethod(@NonNull Class<?> beanClass,@NonNull String staticFactoryMethodName){
        return new GenericBeanDefinition(beanClass,staticFactoryMethodName);
    }

}
