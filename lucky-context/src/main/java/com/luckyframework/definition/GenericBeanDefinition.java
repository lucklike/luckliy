package com.luckyframework.definition;

import com.luckyframework.annotations.Bean;
import com.luckyframework.annotations.ProxyMode;
import com.luckyframework.bean.factory.ConstructorFactoryBean;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.bean.factory.MethodFactoryBean;
import com.luckyframework.bean.factory.StaticMethodFactoryBean;
import com.luckyframework.exception.FactoryBeanCreateException;
import com.luckyframework.proxy.scope.BeanScopePojo;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.lang.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.luckyframework.definition.ClassUtils.getMethodBeanReferenceParameters;
import static com.luckyframework.scanner.Constants.*;
import static com.luckyframework.scanner.ScannerUtils.annotationIsExist;
import static com.luckyframework.scanner.ScannerUtils.getAnnotationAttribute;


/**
 * 通用的Bean定义信息，本类不直接暴露给外界访问
 * 可以通过{@link BeanDefinitionBuilder}的静态方法获取对应的实例
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/4 下午4:03
 */
public class GenericBeanDefinition extends BaseBeanDefinition {

    GenericBeanDefinition(){}

    /**
     * 构造一个Bean的定义信息,使用ConstructorFactoryBean
     * @param beanClassName beanClass的全类名
     */
    GenericBeanDefinition(@NonNull String beanClassName){
        this(ClassUtils.forName(beanClassName, ClassUtils.getDefaultClassLoader()));
    }

    /**
     * 构造一个Bean的定义信息,使用ConstructorFactoryBean
     * @param beanClass bean的Class
     */
    GenericBeanDefinition(@NonNull Class<?> beanClass){
        Constructor<?> constructor = com.luckyframework.definition.ClassUtils.findConstructor(beanClass);
        setFactoryBean(constructorToFactoryBean(constructor));
        setPropertyValue(PropertyInjectionUtils.getInjectionPropertyValues(beanClass));
        setSetterValues(PropertyInjectionUtils.getInjectionSetterValues(beanClass));
        setBeanDefinitionField(AnnotationMetadata.introspect(beanClass));
    }

    /**
     * 构造一个Bean的定义信息,使用MethodFactoryBean
     * @param beanName      bean实例的名称
     * @param factoryMethod 工厂方法的实例
     */
    GenericBeanDefinition(@NonNull String beanName,@NonNull Method factoryMethod){
        setFactoryBean(factoryMethodToFactoryBean(beanName, factoryMethod));
        setBeanDefinitionField(new StandardMethodMetadata(factoryMethod));
    }

    /**
     * 构造一个Bean的定义信息,使用StaticMethodFactoryBean
     * @param beanClass             bean的Class
     * @param staticFactoryMethod   工厂方法的实例
     */
    GenericBeanDefinition(@NonNull Class<?> beanClass,@NonNull Method staticFactoryMethod){
        setFactoryBean(staticFactoryMethodToFactoryBean(beanClass, staticFactoryMethod));
        setBeanDefinitionField(new StandardMethodMetadata(staticFactoryMethod));
    }

    /**
     * 构造一个Bean的定义信息,使用StaticMethodFactoryBean
     * @param beanClassName             beanClass的全类名
     * @param staticFactoryMethodName   工厂方法名称
     */
    GenericBeanDefinition(@NonNull String beanClassName,@NonNull String staticFactoryMethodName){
        this(ClassUtils.forName(beanClassName,ClassUtils.getDefaultClassLoader()),staticFactoryMethodName);
    }

    /**
     * 构造一个Bean的定义信息,使用StaticMethodFactoryBean
     * @param beanClass                 bean的Class
     * @param staticFactoryMethodName   工厂方法名称
     */
    GenericBeanDefinition(@NonNull Class<?> beanClass,@NonNull String staticFactoryMethodName){
        List<Method> allStaticMethod = ClassUtils.getAllStaticMethod(beanClass, staticFactoryMethodName);
        List<Method> hitStaticMethods = new ArrayList<>();
        if(allStaticMethod.size() == 1){
            hitStaticMethods.add(allStaticMethod.get(0));
        }else{
            for (Method method : allStaticMethod) {
                if(AnnotatedElementUtils.isAnnotated(method,Bean.class)){
                    hitStaticMethods.add(method);
                }
            }
        }
        if(hitStaticMethods.size() == 1){
            Method staticFactoryMethod = hitStaticMethods.get(0);
            setFactoryBean(staticFactoryMethodToFactoryBean(beanClass,staticFactoryMethod));
            setBeanDefinitionField(new StandardMethodMetadata(staticFactoryMethod));
        }else if(hitStaticMethods.size() == 0){
            throw new  FactoryBeanCreateException("A static method named '"+staticFactoryMethodName+"' could not be found in '"+beanClass+"'.");
        }else{
            throw new  FactoryBeanCreateException("Multiple static methods named '"+staticFactoryMethodName+"' were found in '"+beanClass+"',and Lucky was unable to determine which to use.");
        }
    }


    /**
     * 将一个具体的构造器转化为ConstructorFactoryBean
     * 1.遍历构造器的参数，并将其转化为BeanReference
     * 2.使用beanClass和BeanReference数组创建ConstructorFactoryBean
     * @param constructor 构造器实例
     * @return {@link ConstructorFactoryBean}
     */
    public FactoryBean constructorToFactoryBean(Constructor<?> constructor){
        Object[] parameters
                = com.luckyframework.definition.ClassUtils.findConstructorBeanReferenceParameters(constructor);
        return new ConstructorFactoryBean(constructor,parameters);
    }


    /**
     * 将一个具体bean的beanName和Method实例转化为MethodFactoryBean
     * @param beanName          bean实例的名称
     * @param factoryMethod     工厂方法实例
     * @return {@link MethodFactoryBean}
     */
    public FactoryBean factoryMethodToFactoryBean(String beanName, Method factoryMethod){
        return new MethodFactoryBean(beanName,factoryMethod,getMethodBeanReferenceParameters(factoryMethod));
    }

    /**
     * 将一个具体bean的beanName和Method实例转化为StaticMethodFactoryBean
     * @param beanClass             bean的Class
     * @param staticFactoryMethod   静态工厂方法实例
     * @return {@link StaticMethodFactoryBean}
     */
    public FactoryBean staticFactoryMethodToFactoryBean(Class<?> beanClass, Method staticFactoryMethod){
        return new StaticMethodFactoryBean(beanClass,staticFactoryMethod,getMethodBeanReferenceParameters(staticFactoryMethod));
    }

    /***
     * 为本bean定义信息设置属性
     * @param scannerElement 扫描元素
     */
    public void setBeanDefinitionField(AnnotatedTypeMetadata scannerElement){
        setBeanDefinitionField(scannerElement,this);
    }

    /***
     * 将注解翻译为对应的bean定义信息的属性，并完成设置
     * @param scannerElement 扫描元素
     * @param componentDefinition 该扫描元素对应的bean定义信息
     */
    public static void setBeanDefinitionField(AnnotatedTypeMetadata scannerElement, BeanDefinition componentDefinition){
        if(annotationIsExist(scannerElement, SCOPE_ANNOTATION_NAME)){
            String beanScope = (String) getAnnotationAttribute(scannerElement, SCOPE_ANNOTATION_NAME,"scopeName");
            ProxyMode proxyMode = (ProxyMode) getAnnotationAttribute(scannerElement, SCOPE_ANNOTATION_NAME,"proxyMode");
            componentDefinition.setScope(new BeanScopePojo(beanScope,proxyMode));
        }
        if(annotationIsExist(scannerElement, LAZY_ANNOTATION_NAME)){
            boolean isLazy = (boolean) getAnnotationAttribute(scannerElement, LAZY_ANNOTATION_NAME, VALUE);
            componentDefinition.setLazyInit(isLazy);
        }
        if(annotationIsExist(scannerElement, PROXY_MODEL_ANNOTATION_NAME)){
            ProxyMode proxyMode = (ProxyMode) getAnnotationAttribute(scannerElement, PROXY_MODEL_ANNOTATION_NAME,VALUE);
            componentDefinition.setProxyMode(proxyMode);
        }
        if(annotationIsExist(scannerElement, PRIMARY_ANNOTATION_NAME)){
            componentDefinition.setPrimary(true);
        }
        if(annotationIsExist(scannerElement, DEPENDS_ON_ANNOTATION_NAME)){
            componentDefinition.setDependsOn((String[]) getAnnotationAttribute(scannerElement, DEPENDS_ON_ANNOTATION_NAME,VALUE));
        }
        if(annotationIsExist(scannerElement,PRIORITY_DESTROY_ANNOTATION_NAME)){
            componentDefinition.setPriority((Integer) getAnnotationAttribute(scannerElement, PRIORITY_DESTROY_ANNOTATION_NAME,VALUE));
        }
        if(annotationIsExist(scannerElement, ORDER_ANNOTATION_NAME)){
            componentDefinition.setPriority((Integer) getAnnotationAttribute(scannerElement, ORDER_ANNOTATION_NAME,VALUE));
        }
        if(scannerElement instanceof AnnotationMetadata){
            String[] initMethodNames = ((AnnotationMetadata) scannerElement)
                    .getAnnotatedMethods(POST_CONSTRUCT_ANNOTATION_NAME)
                    .stream().map(MethodMetadata::getMethodName)
                    .distinct().toArray(String[]::new);
            String[] destroyMethodNames = ((AnnotationMetadata) scannerElement)
                    .getAnnotatedMethods(PRE_DESTROY_ANNOTATION_NAME)
                    .stream().map(MethodMetadata::getMethodName)
                    .distinct().toArray(String[]::new);
            componentDefinition.setInitMethodNames(initMethodNames);
            componentDefinition.setDestroyMethodNames(destroyMethodNames);
        }
    }
}
