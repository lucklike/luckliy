package com.luckyframework.definition;

import com.luckyframework.annotations.*;
import com.luckyframework.bean.factory.*;
import com.luckyframework.exception.FactoryBeanCreateException;
import com.luckyframework.proxy.scope.BeanScopePojo;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Constructor;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        setPropertyValue(getPropertyValues(beanClass));
        setSetterValues(getSetterValues(beanClass));
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

    public PropertyValue[] getPropertyValues(Class<?> beanClass){
        List<Field> byJavaIdFields = ClassUtils.getFieldByAnnotation(beanClass, Resource.class);

        List<Field> byIdFields = ClassUtils.getFieldByAnnotation(beanClass, Qualifier.class);
        byIdFields.removeIf(byJavaIdFields::contains);

        List<Field> byTypeFields = ClassUtils.getFieldByAnnotation(beanClass, Autowired.class);
        byTypeFields.removeIf(f -> byJavaIdFields.contains(f) || byIdFields.contains(f));

        List<Field> byValueFields = ClassUtils.getFieldByAnnotation(beanClass, Value.class);
        byValueFields.removeIf(f -> byJavaIdFields.contains(f) || byIdFields.contains(f) || byTypeFields.contains(f));

        PropertyValue[] propertyValues = new PropertyValue[
            byJavaIdFields.size() + byIdFields.size()+
            byTypeFields.size() + byValueFields.size()
        ];
        int i = 0;
        for (Field field : byJavaIdFields) {

            String fieldName = field.getName();
            Resource resource = AnnotationUtils.get(field,Resource.class);
            BeanReference beanReference = StringUtils.hasText(resource.name())
                    ? BeanReference.builderName(resource.name(), true)
                    : BeanReference.builderAutoNameFirst(fieldName,ResolvableType.forField(field, beanClass), true);
            BeanReferenceUtils.setLazyProperty(field,beanReference);
            propertyValues[i++] = new PropertyValue(fieldName,beanReference,field);
        }
        for (Field field : byIdFields) {
            Qualifier qualifier = AnnotationUtils.get(field, Qualifier.class);
            String fieldName = field.getName();
            BeanReference beanReference = StringUtils.hasText(qualifier.value())
                    ? BeanReference.builderName(qualifier.value(), qualifier.required())
                    : BeanReference.builderAutoNameFirst(fieldName,ResolvableType.forField(field, beanClass), qualifier.required());
            BeanReferenceUtils.setLazyProperty(field,beanReference);
            propertyValues[i++] = new PropertyValue(fieldName,beanReference,field);
        }
        for (Field field : byTypeFields) {
            String fieldName = field.getName();
            Autowired autowired = AnnotationUtils.get(field, Autowired.class);
            BeanReference beanReference = BeanReference.builderAutoTypeFirst(fieldName,ResolvableType.forField(field, beanClass), autowired.required());
            BeanReferenceUtils.setLazyProperty(field,beanReference);
            propertyValues[i++] = new PropertyValue(fieldName,beanReference,field);
        }
        for (Field field : byValueFields) {
            Value value = AnnotationUtils.get(field, Value.class);
            BeanReference beanReference = BeanReference.builderValue(value.value(), ResolvableType.forField(field, beanClass));
            BeanReferenceUtils.setLazyProperty(field,beanReference);
            propertyValues[i++] = new PropertyValue(field.getName(),beanReference,field);
        }
        return propertyValues;
    }

    public SetterValue[] getSetterValues(Class<?> beanClass){
        List<Method> resourceMethods = ClassUtils.getMethodByAnnotation(beanClass, Resource.class);

        List<Method> qualifierMethods = ClassUtils.getMethodByAnnotation(beanClass, Qualifier.class);
        qualifierMethods.removeIf(qualifierMethods::contains);

        List<Method> autowiredMethods = ClassUtils.getMethodByAnnotation(beanClass, Autowired.class);
        autowiredMethods.removeIf(f -> resourceMethods.contains(f) || qualifierMethods.contains(f));

        List<Method> valueMethods = ClassUtils.getMethodByAnnotation(beanClass, Value.class);
        valueMethods.removeIf(f -> resourceMethods.contains(f) || qualifierMethods.contains(f) || autowiredMethods.contains(f));


        for (Method method : resourceMethods) {
            if(method.getParameters().length!=1){
                throw new IllegalStateException("@Resource annotation requires a single-arg method: "+method);
            }
        }

        for (Method method : qualifierMethods) {
            if(method.getParameters().length!=1){
                throw new IllegalStateException("@Qualifier annotation requires a single-arg method: "+method);
            }
        }

        SetterValue[] setterValues = new SetterValue[
            resourceMethods.size() + autowiredMethods.size() +
            valueMethods.size() + qualifierMethods.size()
        ];

        LocalVariableTableParameterNameDiscoverer paramTables = new LocalVariableTableParameterNameDiscoverer();
        int i = 0;
        for (Method method : resourceMethods) {
            Resource resourceQualifier = AnnotationUtils.get(method,Resource.class);
            String resourceName = resourceQualifier.name();
            String parameterName = Objects.requireNonNull(paramTables.getParameterNames(method))[0];
            Parameter parameter = method.getParameters()[0];
            ResolvableType paramType = ResolvableType.forType(method.getGenericParameterTypes()[0]);
            Object[] args = new Object[1];
            BeanReference beanReference = StringUtils.hasText(resourceName)
                    ? BeanReference.builderName(resourceName,true)
                    : BeanReference.builderAutoNameFirst(parameterName,paramType,true);
            BeanReferenceUtils.setLazyProperty(method,parameter,beanReference);
            args[0] = beanReference;
            setterValues[i++] = new SetterValue(method.getName(),method.getParameterTypes(),args,method);
        }
        for (Method method : qualifierMethods) {
            Qualifier qualifier = AnnotationUtils.get(method,Qualifier.class);
            String qualifierValue = qualifier.value();
            String parameterName = Objects.requireNonNull(paramTables.getParameterNames(method))[0];
            Parameter parameter = method.getParameters()[0];
            ResolvableType paramType = ResolvableType.forType(method.getGenericParameterTypes()[0]);
            Object[] args = new Object[1];
            BeanReference beanReference = StringUtils.hasText(qualifierValue)
                    ? BeanReference.builderName(qualifierValue,qualifier.required())
                    : BeanReference.builderAutoNameFirst(parameterName,paramType,qualifier.required());
            BeanReferenceUtils.setLazyProperty(method,parameter,beanReference);
            args[0] = beanReference;
            setterValues[i++] = new SetterValue(method.getName(),method.getParameterTypes(),args,method);
        }
        for (Method method : autowiredMethods) {
            Parameter[] parameters = method.getParameters();
            BeanReference[] beanReferences = null;
            if(parameters != null){

                beanReferences = new BeanReference[parameters.length];
                String[] parameterNames = paramTables.getParameterNames(method);
                Type[] genericParameterTypes = method.getGenericParameterTypes();
                boolean methodRequired = method.getAnnotation(Autowired.class).required();

                for (int j = 0; j < parameters.length; j++) {
                    Parameter parameter = parameters[j];
                    ResolvableType parameterType = ResolvableType.forType(genericParameterTypes[j]);
                    String parameterName = parameterNames[j];

                    // @Resource
                    if(parameter.isAnnotationPresent(Resource.class)){
                        Resource resource = parameter.getAnnotation(Resource.class);
                        beanReferences[j] = StringUtils.hasText(resource.name())
                                ? BeanReference.builderName(resource.name(),true)
                                : BeanReference.builderAutoNameFirst(parameterName,parameterType,true);
                    }
                    // @Qualifier
                    else if(parameter.isAnnotationPresent(Qualifier.class)){
                        Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
                        beanReferences[j] = StringUtils.hasText(qualifier.value())
                                ? BeanReference.builderName(qualifier.value(),qualifier.required())
                                : BeanReference.builderAutoNameFirst(parameterName,parameterType,qualifier.required());
                    }
                    // @Autowired
                    else if (parameter.isAnnotationPresent(Autowired.class)) {
                        beanReferences[j] = BeanReference.builderAutoTypeFirst(parameterName,parameterType,parameter.getAnnotation(Autowired.class).required());
                    }
                    // @Value
                    else if(parameter.isAnnotationPresent(Value.class)){
                        Value value = parameter.getAnnotation(Value.class);
                        beanReferences[j] = BeanReference.builderValue(value.value(),parameterType);
                    }
                    // 参数无注解
                    else{
                        beanReferences[j] = BeanReference.builderAutoTypeFirst(parameterName,parameterType,methodRequired);
                    }
                    BeanReferenceUtils.setLazyProperty(method,parameter, beanReferences[j]);
                }
            }
            setterValues[i++] = new SetterValue(method.getName(),method.getParameterTypes(),beanReferences,method);
        }
        for (Method method : valueMethods) {
            Parameter[] parameters = method.getParameters();
            BeanReference[] beanReferences = null;
            if(parameters != null){

                beanReferences = new BeanReference[parameters.length];
                String methodValue = method.getAnnotation(Value.class).value();
                boolean methodValueIsNotEmpty = StringUtils.hasText(methodValue);
                String[] parameterNames = paramTables.getParameterNames(method);
                Type[] genericParameterTypes = method.getGenericParameterTypes();

                for (int j = 0; j < parameters.length; j++) {
                    String valueExpression;
                    if(methodValueIsNotEmpty){
                        valueExpression = parameters[j].isAnnotationPresent(Value.class)
                                ? parameters[j].getAnnotation(Value.class).value()
                                : methodValue;
                    } else {
                        valueExpression = parameters[j].isAnnotationPresent(Value.class)
                                ? parameters[j].getAnnotation(Value.class).value()
                                : parameterNames[j];
                    }
                    beanReferences[j] = BeanReference.builderValue(valueExpression,ResolvableType.forType(genericParameterTypes[j]));
                    BeanReferenceUtils.setLazyProperty(method,parameters[j], beanReferences[j]);
                }
            }
            setterValues[i++] = new SetterValue(method.getName(),method.getParameterTypes(),beanReferences,method);
        }
        return setterValues;

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
            String[] initMethodNames =
                    ((AnnotationMetadata) scannerElement)
                            .getAnnotatedMethods(POST_CONSTRUCT_ANNOTATION_NAME)
                            .stream().map(MethodMetadata::getMethodName)
                            .distinct().toArray(String[]::new);
            String[] destroyMethodNames =
                    ((AnnotationMetadata) scannerElement)
                            .getAnnotatedMethods(PRE_DESTROY_ANNOTATION_NAME)
                            .stream().map(MethodMetadata::getMethodName)
                            .distinct().toArray(String[]::new);
            componentDefinition.setInitMethodNames(initMethodNames);
            componentDefinition.setDestroyMethodNames(destroyMethodNames);
        }
    }
}
